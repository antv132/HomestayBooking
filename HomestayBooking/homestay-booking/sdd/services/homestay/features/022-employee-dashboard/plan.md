# Implementation Plan: FR-22 Employee Dashboard

**Branch**: `024-employee-dashboard` | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/022-employee-dashboard/spec.md`

**Nguồn docs**: `docs/Specification_v2.md` (FR-22, §10 Employee Dashboard acceptance), `docs/api-spec-by-screen.md` (SCR-59 `GET /employee/kpis`), `docs/screen.md`, `docs/screendesign.md` SCR-59, `docs/component-library.md` (touch-friendly cards)

**Phụ thuộc**: FR-01 (EMPLOYEE auth); FR-20 (employee property assignments); FR-21 (housekeeping_tasks); FR-13 (maintenance_tickets); FR-23 (room_inspections); FR-04 (booking check-out dates). **Ranh giới**: FR-22 owns **composite read** `GET /api/v1/employee/dashboard` + SCR-59 UI; **không** bảng mới; **không** status updates (SCR-60/61/62 thuộc FR-21/13/23).

## Summary

Triển khai **FR-22 Employee Dashboard**: `EmployeeDashboardService` aggregate từ `housekeeping_tasks`, `maintenance_tickets`, `room_inspections`, `bookings` — **không** migration bắt buộc (optional V038 indexes); REST `GET /api/v1/employee/dashboard` trả KPI + preview lists + today/awaiting/completed sections; frontend greenfield `EmployeeDashboardPage.tsx` mobile-first với 3 action cards; route `/employee/dashboard`.

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA (read queries); Vite, Axios, React Router  
**Storage**: SQL Server — read-only joins on FR-21/13/23/04 tables; optional V038 dashboard indexes  
**Testing**: JUnit 5 + Mockito; `EmployeeDashboardServiceTest` KPI counts + today filter; `EmployeeDashboardControllerIT` RBAC + employee scope  
**Target Platform**: Web application (mobile-first for Employee)  
**Project Type**: `Frontend/` + `Backend/`  
**Performance Goals**: Dashboard composite p95 < 3s; single round-trip  
**Constraints**: EMPLOYEE-only; employeeId + property scope on all queries; timezone Asia/Ho_Chi_Minh; read-only  
**Scale/Scope**: 1 REST endpoint (+ optional kpis alias); 6 user stories; SCR-59

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Layered architecture | PASS | Controller → EmployeeDashboardService → repos |
| DTO + Bean Validation | PASS | Response DTO only; no write payloads |
| Security-first (RBAC) | PASS | `@PreAuthorize EMPLOYEE`; employeeId from JWT |
| No secrets in code | PASS | N/A |
| Test coverage ≥80% | PASS | KPI accuracy + scope IT |
| Standard API envelope | PASS | `{ success, message, data }` |
| Audit log | PASS | Read-only — omit v1 |
| Pagination | PASS | Bounded preview lists (5 per type, 3 completed today per type) |

**Post-design re-check**: PASS — no new entities; aggregate service only; consumes FR-21/13/23 repos.

## Project Structure

### Documentation (this feature)

```text
specs/022-employee-dashboard/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/employee-dashboard-api.yaml
├── spec.md
└── tasks.md              # /speckit-tasks (next)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controllers/
│   └── EmployeeDashboardController.java     # GET /api/v1/employee/dashboard
├── dtos/dashboard/
│   ├── EmployeeDashboardResponse.java
│   ├── EmployeeTaskSummaryDto.java          # unified today/awaiting/completed item
│   ├── HousekeepingTaskSummaryDto.java
│   ├── MaintenanceTicketSummaryDto.java
│   └── RoomInspectionSummaryDto.java
├── services/
│   └── EmployeeDashboardService.java        # aggregate KPIs + lists
└── configs/
    └── SecurityConfig.java                  # EMPLOYEE /employee/dashboard

Backend/src/main/resources/db/migration/
└── V038__employee_dashboard_indexes_fr22.sql   # optional

Backend/src/test/java/com/homestay/
├── unit/EmployeeDashboardServiceTest.java
└── integration/EmployeeDashboardControllerIT.java

Frontend/src/
├── api/
│   └── employeeDashboardApi.ts
├── pages/employee/
│   └── EmployeeDashboardPage.tsx            # SCR-59 mobile-first
├── layouts/
│   └── EmployeeLayout.tsx                   # create if missing
└── App.tsx                                  # /employee/dashboard
```

**Structure Decision**: FR-22 **owns** composite dashboard service + SCR-59 page. **Consumes** FR-21/13/23 repositories — no duplicate CRUD or status mutations.

## Implementation Phases

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | Optional V038 indexes on assigned-employee query paths | data-model.md |
| **B** | Dashboard DTOs + `EmployeeDashboardService` | Foundational |
| **C** | `EmployeeDashboardController` + security | US1–US6 |
| **D** | Frontend `employeeDashboardApi` + `EmployeeDashboardPage` | US1–US6 |
| **E** | EmployeeLayout + nav links to SCR-60/61/62 | US1 |
| **F** | Tests + quickstart | SC-001–SC-007 |

## Frontend Gap Analysis

| Item | Hiện tại | Target |
|------|----------|--------|
| SCR-59 page | **missing** | `EmployeeDashboardPage.tsx` |
| API client | **missing** | `employeeDashboardApi.ts` |
| Route | **missing** | `/employee/dashboard` |
| Employee layout | **missing** | `EmployeeLayout.tsx` |
| Action cards | **missing** | 3 touch-friendly cards → SCR-60/61/62 |
| KPI row | **missing** | pendingHousekeeping/Maintenance/Inspections |
| Today section | **missing** | unified today tasks list |
| Awaiting vs completed | **missing** | two summary sections |

## Risks

| Risk | Mitigation |
|------|------------|
| FR-21/13/23 not implemented | Blocker — seed data per dependency quickstarts |
| api-spec only `GET /employee/kpis` | Extend with full composite `/employee/dashboard`; kpis fields subset |
| FR-23 inspection assignment unclear | Filter `inspected_by = employeeId`; property scope via FR-20 |
| N+1 on composite | Single service method; parallel repo calls; optional V038 indexes |
| No Employee frontend shell | Create minimal `EmployeeLayout.tsx` in FR-22 |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/employee-dashboard-api.yaml](./contracts/employee-dashboard-api.yaml)
- [quickstart.md](./quickstart.md)

## Next Step

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md`.
