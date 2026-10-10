# Implementation Plan: FR-06 Property Management

**Branch**: `008-property-management` | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/006-property-management/spec.md`

**Nguồn docs**: `docs/Specification_v2.md` (FR-06, §5 Property/ManagerPropertyAssignment, §10 Admin Management), `docs/api-spec-by-screen.md` (SCR-46–50), `docs/screen.md`, `docs/entity-ui-mapping.md` §1.2, frontend `propertyApi.ts`, `PropertyListPage.tsx`, `AddPropertyPage.tsx`, `PropertyDetailPage.tsx`, `EditPropertyPage.tsx`

**Phụ thuộc**: FR-01 (Admin/Manager auth, RBAC, `GET /admin/users?role=MANAGER` SCR-50). **Enables**: FR-03 (ACTIVE property filter), FR-07/08 (Property/Floor/Room hierarchy), FR-04/05 (property-scoped ops). **Ranh giới**: FR-07 Structure; FR-08 Room CRUD; EmployeePropertyAssignment (FR-17/39); PricingRule seed (entity §5 — optional later); hard delete property.

## Summary

Triển khai **FR-06 Property Management**: Admin CRUD property + ACTIVE/INACTIVE; Admin gán Manager qua `ManagerPropertyAssignment` (một ACTIVE/property, lịch sử INACTIVE); Manager xem list/detail property được gán (read-only). Stack: **Spring Boot 3 + JPA + SQL Server** + **React/TypeScript** — tạo **Admin pages mới** (SCR-46–49), refactor **Manager property pages** từ CRUD sang read-only scoped API.

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA, Flyway; Vite, Axios, React Router, React Query (optional)  
**Storage**: SQL Server — `properties`, `manager_property_assignments`; read `users` (MANAGER role), aggregate `floors`/`rooms` for manager detail stats (stub 0 until FR-07/08)  
**Testing**: JUnit 5 + Mockito; `@SpringBootTest` AdminPropertyControllerIT + ManagerPropertyScopeIT; Vitest for admin form validation  
**Target Platform**: Web application  
**Project Type**: `Frontend/` + `Backend/` (backend scaffold per FR-01)  
**Performance Goals**: Admin list/search p95 < 500ms for ≤500 properties (SC-004); assignment transaction atomic  
**Constraints**: Property ACTIVE ⟹ Manager ACTIVE assignment; envelope `{ success, message, data }`; partial unique index one ACTIVE assignment per property; default INACTIVE on create  
**Scale/Scope**: ~8 REST endpoints; 5 user stories; SCR-46–49 (+ SCR-50 manager picker); 4 new admin pages + manager page refactor

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Layered architecture (Controller → Service → Repository) | PASS | AGENTS.md |
| DTO + Bean Validation | PASS | CreatePropertyRequest, AssignManagerRequest |
| Security-first (RBAC, property isolation) | PASS | Admin-only write; Manager scoped read |
| No secrets in code | PASS | N/A |
| `@Transactional` for assignment swap | PASS | deactivate old + insert new |
| Test coverage ≥80% | PASS | Assignment rules + scope IT |
| Standard API envelope + pagination | PASS | api-spec §1 |
| Audit log PROPERTY_* / MANAGER_ASSIGNED | PASS | ActivityLog |

**Post-design re-check**: PASS — DB partial unique constraint enforces FR-006; FR-003 discovery filter documented as integration hook.

## Project Structure

### Documentation (this feature)

```text
specs/006-property-management/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/property-api.yaml
├── spec.md
└── tasks.md              # /speckit-tasks (next)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controllers/
│   ├── AdminPropertyController.java        # SCR-46–49 /admin/properties/*
│   └── ManagerPropertyController.java      # Manager scoped read
├── dtos/property/
│   ├── CreatePropertyRequest.java
│   ├── UpdatePropertyRequest.java
│   ├── PropertySummaryResponse.java
│   ├── PropertyDetailResponse.java
│   ├── AssignManagerRequest.java
│   ├── ManagerAssignmentResponse.java
│   └── PropertyPageResponse.java
├── entities/
│   ├── Property.java
│   └── ManagerPropertyAssignment.java
├── enums/PropertyStatus.java, AssignmentStatus.java
├── repositories/
│   ├── PropertyRepository.java
│   └── ManagerPropertyAssignmentRepository.java
├── services/
│   ├── PropertyService.java              # CRUD, activate/deactivate rules
│   ├── ManagerAssignmentService.java     # assign, history, swap
│   └── PropertyStatsService.java         # floor/room counts for detail
├── security/PropertyAccessValidator.java # reuse for manager scope
└── configs/SecurityConfig.java             # ADMIN / MANAGER routes

Backend/src/main/resources/db/migration/
├── V005__properties.sql
└── V006__manager_property_assignments.sql

Backend/src/test/java/com/homestay/
├── unit/ManagerAssignmentServiceTest.java
├── unit/PropertyActivationServiceTest.java
└── integration/AdminPropertyControllerIT.java

Frontend/src/
├── api/
│   ├── adminPropertyApi.ts                 # NEW — /api/v1/admin/properties/*
│   └── propertyApi.ts                      # refactor → /api/v1/manager/properties/*
├── layouts/AdminLayout.tsx                 # NEW if missing
├── pages/admin/
│   ├── PropertyManagementPage.tsx          # SCR-46
│   ├── CreatePropertyPage.tsx              # SCR-47
│   ├── EditPropertyPage.tsx                # SCR-48
│   └── AssignManagerPage.tsx               # SCR-49
├── pages/manager/
│   ├── PropertyListPage.tsx                # read-only, scoped
│   ├── PropertyDetailPage.tsx              # read-only detail + stats
│   └── _propertyShared.tsx                 # shared form → admin only
└── App.tsx                                 # admin routes + role guards
```

**Structure Decision**: FR-06 **owns** `properties` + `manager_property_assignments` tables (V005–V006, before FR-03 seed V010). Split Admin vs Manager API surfaces; frontend currently treats Manager as CRUD — **must remove** Manager create/edit/delete per spec FR-010.

## Implementation Phases

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | Flyway V005 properties, V006 manager_property_assignments + partial unique index | data-model.md |
| **B** | Entities, PropertyRepository, PropertyService create/update | US-1, FR-001–003 |
| **C** | AdminPropertyController GET list (search, filter, pagination, manager join) | US-5, FR-004 |
| **D** | POST/PUT admin properties; ACTIVE gate (requires manager) | US-1, US-4, FR-008 |
| **E** | ManagerAssignmentService + PATCH assign manager | US-2, FR-005–007, FR-012 |
| **F** | ManagerPropertyController GET list/detail scoped by assignment | US-3, FR-009–010 |
| **G** | PropertyStatsService aggregates (floors/rooms — 0 if empty) | US-3 detail |
| **H** | ActivityLog PROPERTY_CREATED, MANAGER_ASSIGNED, PROPERTY_STATUS_CHANGED | audit |
| **I** | Admin frontend SCR-46–49 + adminPropertyApi | US-1, US-2, US-5 |
| **J** | Refactor manager pages: remove create/delete; migrate propertyApi paths | US-3, FR-010 |
| **K** | FR-03 hook: discovery excludes INACTIVE properties | FR-011 integration |
| **L** | Tests + quickstart | SC-001–SC-006 |

## Frontend Gap Analysis

| Item | Hiện tại | Target |
|------|----------|--------|
| Admin UI | Không có `pages/admin/` | SCR-46–49 dedicated pages |
| Manager create property | `AddPropertyPage`, `propertyApi.create` | **Remove** — Admin only (FR-010) |
| Manager delete | `PropertyListPage` delete action | **Remove** — INACTIVE via Admin |
| API paths | `/api/properties` | `/api/v1/admin/properties`, `/api/v1/manager/properties` |
| api-spec field | `location` | Map to `address` in DTO |
| List columns | Manager list only | Admin list + current Manager name column |
| Assign Manager | Không có UI | SCR-49 page + SCR-50 manager dropdown |
| Default status on create | May default ACTIVE | **INACTIVE** until manager + explicit activate |

## Risks

| Risk | Mitigation |
|------|------------|
| Manager pages built as CRUD | Refactor in Phase J; keep shared `PropertyForm` for Admin only |
| FR-03 seed V010 before V005 | Order migrations V005–V006 before V010 |
| Activate without manager race | DB check + `@Transactional` in PropertyService |
| Suspended Manager still assigned | Allow assignment; FR-01 blocks login; Admin UI warning badge |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/property-api.yaml](./contracts/property-api.yaml)
- [quickstart.md](./quickstart.md)

## Next Step

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md`.
