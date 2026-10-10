# Implementation Plan: FR-07 Structure Management

**Branch**: `009-structure-management` | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/007-structure-management/spec.md`

**Nguồn docs**: `docs/Specification_v2.md` (FR-07, §5 Floor), `docs/api-spec-by-screen.md` (SCR-28), `docs/screen.md`, `docs/screendesign.md`, `docs/entity-ui-mapping.md` §1.3, `docs/component-library.md` TreeView, frontend `StructureTreePage.tsx`, `floorApi.ts`, `propertyApi.getStructure`

**Phụ thuộc**: FR-06 (Property + Manager assignment scope). **Ranh giới**: FR-08 Room CRUD/gallery/status; FR-03 discovery; Employee SCR-65. Tree **reads** Room rows when FR-08 `rooms` table exists — empty room arrays until then.

## Summary

Triển khai **FR-07 Structure Management**: bảng `floors`, Floor CRUD (Manager, property-scoped), structure tree API `Property → Floor → Room` (rooms read-only), Property Selector (assigned properties), Admin read-only tree. Stack: **Spring Boot 3 + JPA + SQL Server** + **React/TypeScript** — enrich existing `StructureTreePage.tsx`; migrate `floorApi.ts` và `propertyApi.getStructure` → `/api/v1`.

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA, Flyway; Vite, Axios, React Router  
**Storage**: SQL Server — `floors` (new V008); read `properties`, `manager_property_assignments` (FR-06); read `rooms` (FR-08, optional join)  
**Testing**: JUnit 5 + Mockito; `@SpringBootTest` StructureTreeControllerIT, FloorControllerIT; duplicate floor number unit tests  
**Target Platform**: Web application  
**Project Type**: `Frontend/` + `Backend/`  
**Performance Goals**: Tree load ≤200 rooms in <3s user-facing (SC-005); single query tree with JOIN FETCH or DTO projection  
**Constraints**: Unique `(property_id, floor_number)`; delete floor only if `room_count=0`; `@PropertyAccess` on Manager writes; Admin read-only no floor writes  
**Scale/Scope**: ~6 REST endpoints; 5 user stories; SCR-28 (+ optional `/manager/floors` reuse)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Layered architecture | PASS | StructureController → FloorService → repositories |
| DTO + Bean Validation | PASS | CreateFloorRequest floorNumber @Min(1) |
| Security-first (RBAC, property isolation) | PASS | FR-007 spec; PropertyAccessValidator |
| No secrets in code | PASS | N/A |
| Test coverage ≥80% | PASS | Floor validation + scope IT |
| Standard API envelope | PASS | api-spec §1 |
| Audit log FLOOR_* | PASS | create/update/delete floor |

**Post-design re-check**: PASS — unique constraint at DB + service; room nodes delegated read to RoomRepository (FR-08).

## Project Structure

### Documentation (this feature)

```text
specs/007-structure-management/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/structure-api.yaml
├── spec.md
└── tasks.md              # /speckit-tasks (next)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controllers/
│   ├── StructureTreeController.java    # GET .../tree (Manager + Admin)
│   └── ManagerFloorController.java     # CRUD /manager/floors
├── dtos/structure/
│   ├── CreateFloorRequest.java
│   ├── UpdateFloorRequest.java
│   ├── FloorResponse.java
│   ├── StructureTreeResponse.java
│   ├── FloorTreeNode.java
│   └── RoomTreeNode.java
├── entities/Floor.java
├── repositories/FloorRepository.java
├── services/
│   ├── FloorService.java               # CRUD + unique validation
│   └── StructureTreeService.java       # assemble tree, sort by floorNumber
├── security/PropertyAccessValidator.java  # reuse FR-06
└── configs/SecurityConfig.java

Backend/src/main/resources/db/migration/
└── V008__floors.sql

Backend/src/test/java/com/homestay/
├── unit/FloorServiceTest.java
└── integration/StructureTreeControllerIT.java

Frontend/src/
├── api/floorApi.ts                     # migrate → /api/v1/manager/floors
├── api/propertyApi.ts                  # getStructure → GET /api/v1/properties/{id}/tree
├── pages/manager/StructureTreePage.tsx # SCR-28 — align API + 8-status badges (FR-05)
└── pages/admin/StructureTreePage.tsx   # optional US5 read-only OR role flag on shared page
```

**Structure Decision**: FR-07 owns `floors` table (V008 after FR-06 V005–V007). Frontend `StructureTreePage.tsx` already implements tree UI + FloorModal — primary work is backend + API path alignment.

## Implementation Phases

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | Flyway V008 floors + unique index | data-model.md |
| **B** | Floor entity, FloorRepository, DTOs | Foundational |
| **C** | StructureTreeService + GET tree | US-1, FR-001, FR-008, FR-010 |
| **D** | FloorService create/update/delete | US-2, US-3, US-4, FR-003–006 |
| **E** | ManagerFloorController + StructureTreeController | US-1–4 |
| **F** | Admin read-only tree + selector all properties | US-5, FR-009 |
| **G** | ActivityLog FLOOR_CREATED/UPDATED/DELETED | audit |
| **H** | Frontend floorApi + StructureTreePage migration | US-1–4 |
| **I** | Admin read-only UI mode | US-5 |
| **J** | Tests + quickstart | SC-001–SC-006 |

## Frontend Gap Analysis

| Item | Hiện tại | Target |
|------|----------|--------|
| Tree API | `GET /api/properties/{id}/structure` | `GET /api/v1/properties/{id}/tree` per api-spec SCR-28 |
| Floor CRUD | `/api/floors` | `/api/v1/manager/floors` |
| Property selector | `propertyApi.getAll` legacy | `/api/v1/manager/properties` (FR-06) |
| Room status on tree | 5 statuses | Extend to 8 when FR-05 constants available |
| Admin tree | Không có | Read-only mode US-5 |
| Route | `/manager/structure` MANAGER only | Admin route or shared component with role |

## Risks

| Risk | Mitigation |
|------|------------|
| FR-08 rooms table missing | Tree returns floors with empty `rooms[]`; seed in FR-08 |
| FR-06 not implemented | Blocker for property scope — implement FR-06 first or mock assignment in IT |
| Duplicate SCR-37 vs SCR-28 | Single tree endpoint; deprecate `/structure` alias optional |
| N+1 tree query | Single JPQL with left join rooms ordered by floorNumber, roomNumber |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/structure-api.yaml](./contracts/structure-api.yaml)
- [quickstart.md](./quickstart.md)

## Next Step

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md`.
