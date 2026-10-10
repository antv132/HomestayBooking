# Implementation Plan: FR-21 Housekeeping Management

**Branch**: `023-housekeeping-management` | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/021-housekeeping-management/spec.md`

**Nguồn docs**: `docs/Specification_v2.md` (FR-21, §5 HousekeepingTask, §10 Housekeeping), `docs/api-spec-by-screen.md` (SCR-40/SCR-60), `docs/screen.md`, `docs/screendesign.md`, `docs/entity-ui-mapping.md` §1.10

**Phụ thuộc**: FR-04 (Booking Checked-out hook); FR-23 (inspection before checkout); FR-12 (payments settled); FR-08 (Room status); FR-20 (Employee property assignment); FR-06 (Manager property scope). **Ranh giới**: FR-22 Dashboard counts only; FR-13 Maintenance; FR-04 owns checkout UI; Admin housekeeping **read-only**.

## Summary

Triển khai **FR-21 Housekeeping Management**: Flyway **V037** bảng `housekeeping_tasks`; `HousekeepingTaskService.onBookingCheckedOut(bookingId)` auto-create Pending task + room **Pending Cleaning**; Manager SCR-40 list/board + assign/create/cancel; Employee SCR-60 start/complete; room sync **Cleaning In Progress** → **Available**; guard FR-08 manual **Available** bypass; REST Manager/Employee + Admin read-only list.

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA, Bean Validation; Vite, Axios, React Router  
**Storage**: SQL Server — `housekeeping_tasks` (V037); read/update `rooms` (FR-08); read `bookings` (FR-04 hook)  
**Testing**: JUnit 5 + Mockito; `HousekeepingTaskServiceTest` transitions + idempotency; `HousekeepingControllerIT` RBAC + room sync  
**Target Platform**: Web application  
**Project Type**: `Frontend/` + `Backend/`  
**Performance Goals**: Task list p95 < 2s; status update < 1s  
**Constraints**: Strict Pending→In Progress→Completed; Manager property-scoped; Employee assignee-only updates; no manual Available bypass  
**Scale/Scope**: ~10 REST endpoints; 6 user stories; SCR-40 + SCR-60

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Layered architecture | PASS | Controllers → HousekeepingTaskService → repos |
| DTO + Bean Validation | PASS | Create/Assign/UpdateStatus requests |
| Security-first (RBAC) | PASS | MANAGER property scope; EMPLOYEE assignee; ADMIN read-only |
| No secrets in code | PASS | N/A |
| Test coverage ≥80% | PASS | Transitions + bypass guard IT |
| Standard API envelope | PASS | `{ success, message, data }` |
| Audit log | PASS | HOUSEKEEPING_* events via ActivityLogService |
| Pagination | PASS | Manager list paginated |

**Post-design re-check**: PASS — room status coupling documented; checkout hook owned by FR-21 service method.

## Project Structure

### Documentation (this feature)

```text
specs/021-housekeeping-management/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/housekeeping-api.yaml
├── spec.md
└── tasks.md              # /speckit-tasks (next)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controllers/
│   ├── ManagerHousekeepingController.java    # SCR-40
│   ├── EmployeeHousekeepingController.java   # SCR-60
│   └── AdminHousekeepingController.java      # read-only list
├── dtos/housekeeping/
│   ├── HousekeepingTaskResponse.java
│   ├── CreateHousekeepingTaskRequest.java
│   ├── AssignHousekeepingTaskRequest.java
│   ├── UpdateHousekeepingStatusRequest.java
│   └── CancelHousekeepingTaskRequest.java
├── entities/
│   └── HousekeepingTask.java
├── enums/
│   └── HousekeepingTaskStatus.java
├── repositories/
│   └── HousekeepingTaskRepository.java
├── services/
│   ├── HousekeepingTaskService.java          # lifecycle + onBookingCheckedOut
│   └── RoomStatusGuardService.java           # block manual Available bypass
└── configs/
    └── SecurityConfig.java

Backend/src/main/resources/db/migration/
└── V037__housekeeping_tasks_fr21.sql

Backend/src/test/java/com/homestay/
├── unit/HousekeepingTaskServiceTest.java
└── integration/HousekeepingControllerIT.java

Frontend/src/
├── api/
│   └── housekeepingApi.ts
├── pages/manager/
│   └── HousekeepingTasksPage.tsx             # SCR-40 board
├── pages/employee/
│   └── HousekeepingWorkspacePage.tsx         # SCR-60 list
├── components/housekeeping/
│   ├── HousekeepingTaskCard.tsx
│   └── AssignHousekeepingDrawer.tsx
└── App.tsx                                   # /manager/housekeeping, /employee/housekeeping
```

**Structure Decision**: FR-21 **owns** `housekeeping_tasks` (V037). **Hooks** into FR-04 checkout via `onBookingCheckedOut`. **Extends** FR-08 room status with housekeeping-driven transitions + bypass guard.

## Implementation Phases

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | V037 migration + HousekeepingTaskStatus enum | data-model.md |
| **B** | HousekeepingTaskService + room sync | Foundational |
| **C** | Auto-create on checkout hook | US1 |
| **D** | Manager list/board + Admin read | US2 |
| **E** | Manager assign + manual create | US3 |
| **F** | Employee SCR-60 start/complete | US4, US5 |
| **G** | Cancel + history filter | US6 |
| **H** | Room bypass guard in FR-08 | US5, FR-009 |
| **I** | Frontend SCR-40/SCR-60 + tests | All |

## Frontend Gap Analysis

| Item | Hiện tại | Target |
|------|----------|--------|
| SCR-40 page | **missing** | `HousekeepingTasksPage.tsx` |
| SCR-60 page | **missing** | `HousekeepingWorkspacePage.tsx` |
| API client | **missing** | `housekeepingApi.ts` |
| Routes | **missing** | `/manager/housekeeping`, `/employee/housekeeping` |

## Risks

| Risk | Mitigation |
|------|------------|
| FR-04 checkout not implemented | Stub hook + integration test with manual task create |
| FR-23 inspection gate missing | Document precondition; hook only after valid checkout |
| FR-08 room update bypass | `RoomStatusGuardService` central check |
| Race on duplicate auto-create | Unique index on `booking_id` where status not CANCELLED |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/housekeeping-api.yaml](./contracts/housekeeping-api.yaml)
- [quickstart.md](./quickstart.md)

## Next Step

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md`.
