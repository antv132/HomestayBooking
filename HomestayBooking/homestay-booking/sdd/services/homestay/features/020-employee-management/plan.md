# Implementation Plan: FR-20 Employee Management

**Branch**: `022-employee-management` | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/020-employee-management/spec.md`

**Nguồn docs**: `docs/Specification_v2.md` (FR-20, §5 User & EmployeePropertyAssignment), `docs/api-spec-by-screen.md` (SCR-39), `docs/screen.md`, `docs/screendesign.md`, `docs/entity-ui-mapping.md` §1.1 User SCR-39

**Phụ thuộc**: FR-01 (`users` role EMPLOYEE, SUSPENDED login block, invite/reset email); FR-06 (`properties`, `manager_property_assignments`, `PropertyScopeService`). **Ranh giới**: FR-21 Housekeeping assignment; FR-22 Employee Dashboard; FR-13 Maintenance assignment; FR-23 Inspection; Employee self-profile FR-02.

## Summary

Triển khai **FR-20 Employee Management**: Flyway **V036** bảng `employee_property_assignments`; REST Manager `GET/POST /api/v1/manager/employees/**` + assign; REST Admin mirror `/api/v1/admin/employees/**` + reassign P2; `EmployeeManagementService` với property scope (Manager) vs global (Admin); tạo user EMPLOYEE + assignment ACTIVE trong transaction; frontend **mới** `EmployeeMgmtPage.tsx` SCR-39 tại `/manager/employees` + `/admin/employees` với Property picker (Admin).

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA, Bean Validation; Vite, Axios, React Router  
**Storage**: SQL Server — `employee_property_assignments` (V036); read/write `users` (FR-01); read `properties` (FR-06)  
**Testing**: JUnit 5 + Mockito; `EmployeeManagementServiceTest` one-active constraint + scope; `EmployeeManagementControllerIT` RBAC + cross-property denial  
**Target Platform**: Web application  
**Project Type**: `Frontend/` + `Backend/`  
**Performance Goals**: Employee list p95 < 2s; assign/create < 3s  
**Constraints**: Manager property-scoped only; one ACTIVE assignment per employee; email immutable v1; no hard delete v1 (Suspend)  
**Scale/Scope**: ~12 REST endpoints; 6 user stories; SCR-39

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Layered architecture | PASS | Controllers → EmployeeManagementService → repos |
| DTO + Bean Validation | PASS | CreateEmployeeRequest, AssignEmployeeRequest, UpdateEmployeeRequest |
| Security-first (RBAC) | PASS | MANAGER property scope; ADMIN global; deny others |
| No secrets in code | PASS | Invite via FR-01 email service |
| Test coverage ≥80% | PASS | Constraint + RBAC IT |
| Standard API envelope | PASS | `{ success, message, data }` |
| Audit log | PASS | EMPLOYEE_* events via ActivityLogService (FR-17 pattern) |
| Pagination | PASS | List paginated with search |

**Post-design re-check**: PASS — assignment table owned by FR-20; reuses FR-01 users + FR-06 scope validator.

## Project Structure

### Documentation (this feature)

```text
specs/020-employee-management/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/employee-api.yaml
├── spec.md
└── tasks.md              # /speckit-tasks (next)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controllers/
│   ├── ManagerEmployeeController.java       # SCR-39 manager APIs
│   └── AdminEmployeeController.java         # SCR-39 admin APIs + reassign P2
├── dtos/employee/
│   ├── EmployeeSummaryResponse.java
│   ├── CreateEmployeeRequest.java
│   ├── AssignEmployeeRequest.java
│   ├── UpdateEmployeeRequest.java
│   ├── UpdateEmployeeStatusRequest.java
│   └── ReassignEmployeeRequest.java         # P2
├── entities/
│   └── EmployeePropertyAssignment.java
├── repositories/
│   ├── EmployeePropertyAssignmentRepository.java
│   └── UserRepository.java                  # extend employee queries (FR-01 base)
├── services/
│   ├── EmployeeManagementService.java
│   └── PropertyScopeService.java            # reuse/extend FR-06
└── configs/
    └── SecurityConfig.java                  # MANAGER /manager/employees/**; ADMIN /admin/employees/**

Backend/src/main/resources/db/migration/
└── V036__employee_property_assignments_fr20.sql

Backend/src/test/java/com/homestay/
├── unit/EmployeeManagementServiceTest.java
└── integration/EmployeeManagementControllerIT.java

Frontend/src/
├── api/
│   └── employeeApi.ts                       # manager + admin employee endpoints
├── pages/manager/
│   └── EmployeeMgmtPage.tsx                 # SCR-39 Staff Directory (new)
├── pages/admin/
│   └── EmployeeMgmtPage.tsx                 # SCR-39 + Property picker (or shared)
├── components/employee/
│   ├── AssignEmployeeModal.tsx
│   └── CreateEmployeeModal.tsx
└── App.tsx                                  # /manager/employees, /admin/employees
```

**Structure Decision**: FR-20 **owns** `employee_property_assignments` (V036). **Consumes** FR-01 `users` and FR-06 property scope. Split Manager vs Admin controller surfaces (align FR-06/FR-09). Frontend **greenfield** — no existing Employee pages.

## Implementation Phases

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | V036 migration + partial unique index | data-model.md |
| **B** | Entity, repository, DTOs, EmployeeManagementService | Foundational |
| **C** | ManagerEmployeeController + security | US1–US5 |
| **D** | AdminEmployeeController + reassign P2 | US2, US6 |
| **E** | Frontend employeeApi + EmployeeMgmtPage + modals | US1–US5 |
| **F** | Admin route + property picker | US6, Admin scope |
| **G** | Tests + quickstart | SC-001–SC-007 |

## Frontend Gap Analysis

| Item | Hiện tại | Target |
|------|----------|--------|
| SCR-39 page | **missing** | `EmployeeMgmtPage.tsx` |
| API client | **missing** | `employeeApi.ts` |
| Routes | **missing** | `/manager/employees`, `/admin/employees` |
| Assign modal | **missing** | Create + assign existing flows |
| Nav link | **missing** | ManagerLayout + AdminLayout |

## Risks

| Risk | Mitigation |
|------|------------|
| FR-01 user creation not implemented | Blocker — implement minimal admin create user or stub invite |
| FR-06 PropertyScopeService missing | Implement inline validator in FR-20 or complete FR-06 first |
| Duplicate ACTIVE assignment race | DB partial unique index + transactional assign |
| Assign suspended employee | Service rejects SUSPENDED/INACTIVE targets |
| Open tasks on reassign P2 | Warning modal only v1 — no auto-cancel |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/employee-api.yaml](./contracts/employee-api.yaml)
- [quickstart.md](./quickstart.md)

## Next Step

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md`.
