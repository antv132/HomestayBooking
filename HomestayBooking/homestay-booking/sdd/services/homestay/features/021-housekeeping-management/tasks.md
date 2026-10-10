# Tasks: FR-21 Housekeeping Management

**Input**: Design documents from `specs/021-housekeeping-management/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/housekeeping-api.yaml, quickstart.md

**Phụ thuộc**: FR-04 (checkout hook); FR-08 (room status); FR-20 (employee property assignment); FR-06 (Manager property scope); FR-12/FR-23 (checkout preconditions upstream). **Ranh giới**: FR-22 Dashboard counts; FR-13 Maintenance; Admin housekeeping read-only.

**Tests**: Không có phase test riêng per-story. Unit + integration trong Phase Polish per plan Phase I.

**Organization**: Tasks grouped by user story (US1–US6) for independent delivery.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no blocking deps)
- **[Story]**: US1–US6 maps to spec.md user stories

## Path Conventions

- **Backend**: `Backend/src/main/java/com/homestay/`
- **Backend tests**: `Backend/src/test/java/com/homestay/`
- **Frontend**: `Frontend/src/`
- **Migrations**: `Backend/src/main/resources/db/migration/`
- **Contract**: `specs/021-housekeeping-management/contracts/housekeeping-api.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Blockers, V037 migration, dev proxy

- [X] T001 Verify FR-01 JWT auth with **MANAGER** and **EMPLOYEE** roles per `specs/001-user-auth/quickstart.md` (blocker)
- [X] T002 Verify FR-06 property scope, FR-08 rooms, FR-20 employee assignments per respective quickstarts (blocker)
- [ ] T003 Create Flyway `Backend/src/main/resources/db/migration/V037__housekeeping_tasks_fr21.sql` — table `housekeeping_tasks`, indexes, `uq_hk_booking_active` per `data-model.md`
- [X] T004 [P] Confirm Vite proxy `/api/v1` → backend in `Frontend/vite.config.ts`
- [ ] T005 [P] Add dev seed SQL comments in V037 — sample PENDING task + checked-out booking reference for local demo

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Entity, enum, repository, DTOs, service skeleton, security — MUST complete before user story phases

**⚠️ CRITICAL**: No user story work until this phase complete

- [X] T006 [P] Create `HousekeepingTaskStatus.java` enum in `Backend/src/main/java/com/homestay/enums/HousekeepingTaskStatus.java` — PENDING, IN_PROGRESS, COMPLETED, CANCELLED
- [X] T007 [P] Create `HousekeepingTask.java` entity in `Backend/src/main/java/com/homestay/entities/HousekeepingTask.java` per `data-model.md`
- [X] T008 [P] Create `HousekeepingTaskRepository.java` in `Backend/src/main/java/com/homestay/repositories/HousekeepingTaskRepository.java` — `findByPropertyIdAndStatus`, `findByAssignedEmployeeIdAndStatus`, `existsActiveByBookingId`, `findOpenByRoomId`
- [X] T009 [P] Create `HousekeepingTaskResponse.java` in `Backend/src/main/java/com/homestay/dtos/housekeeping/HousekeepingTaskResponse.java`
- [X] T010 [P] Create `CreateHousekeepingTaskRequest.java` in `Backend/src/main/java/com/homestay/dtos/housekeeping/CreateHousekeepingTaskRequest.java`
- [X] T011 [P] Create `AssignHousekeepingTaskRequest.java` in `Backend/src/main/java/com/homestay/dtos/housekeeping/AssignHousekeepingTaskRequest.java`
- [ ] T012 [P] Create `UpdateHousekeepingStatusRequest.java` in `Backend/src/main/java/com/homestay/dtos/housekeeping/UpdateHousekeepingStatusRequest.java`
- [X] T013 [P] Create `CancelHousekeepingTaskRequest.java` in `Backend/src/main/java/com/homestay/dtos/housekeeping/CancelHousekeepingTaskRequest.java`
- [X] T014 Create `HousekeepingTaskService.java` skeleton in `Backend/src/main/java/com/homestay/services/HousekeepingTaskService.java` — inject repos, RoomRepository, PropertyScopeService
- [X] T015 Create `RoomStatusGuardService.java` skeleton in `Backend/src/main/java/com/homestay/services/RoomStatusGuardService.java` — `assertCanSetAvailable(roomId)`
- [X] T016 Register `hasRole('MANAGER')` for `/api/v1/manager/housekeeping-tasks/**`, `hasRole('EMPLOYEE')` for `/api/v1/employee/housekeeping-tasks/**`, `hasRole('ADMIN')` GET-only for `/api/v1/admin/housekeeping-tasks` in `Backend/src/main/java/com/homestay/configs/SecurityConfig.java`
- [X] T017 [P] Create `housekeepingApi.ts` skeleton in `Frontend/src/api/housekeepingApi.ts` — typed methods per `contracts/housekeeping-api.yaml`

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Tự động tạo tác vụ sau Check-out (Priority: P1) 🎯 MVP

**Goal**: Auto-create PENDING HousekeepingTask + room Pending Cleaning on booking Checked-out (idempotent)

**Independent Test**: After valid checkout, one PENDING task exists per booking; room Pending Cleaning; no duplicate on retry

### Implementation

- [X] T018 [US1] Implement `onBookingCheckedOut(UUID bookingId)` — load booking/room/property, create PENDING task, set room `PENDING_CLEANING` in `HousekeepingTaskService.java`
- [X] T019 [US1] Add idempotency check via `existsActiveByBookingId` + unique index handling in `HousekeepingTaskService.java`
- [X] T020 [US1] Wire `HousekeepingTaskService.onBookingCheckedOut(bookingId)` call from FR-04 checkout completion in `BookingService.java` (or checkout handler in FR-04 package)
- [X] T021 [US1] Add private `syncRoomStatusForTask(HousekeepingTask)` helper for room transitions in `HousekeepingTaskService.java`
- [X] T022 [US1] Document checkout precondition (inspection + payments) as upstream FR-23/FR-12 gate in `HousekeepingTaskService.java` javadoc

**Checkpoint**: US1 MVP — auto task after checkout testable via manual hook or FR-04 integration

---

## Phase 4: User Story 2 — Manager xem danh sách tác vụ (Priority: P1)

**Goal**: SCR-40 board/list by property; Admin read-only list

**Independent Test**: Manager `GET /manager/housekeeping-tasks?propertyId=` returns scoped tasks; Admin GET read-only; cross-property 403

### Implementation

- [X] T023 [US2] Implement `listByProperty(propertyId, status, fromDate, toDate, page, size, actor)` in `HousekeepingTaskService.java`
- [X] T024 [US2] Create `ManagerHousekeepingController.java` with `GET /api/v1/manager/housekeeping-tasks` in `Backend/src/main/java/com/homestay/controllers/ManagerHousekeepingController.java`
- [X] T025 [US2] Create `AdminHousekeepingController.java` with read-only `GET /api/v1/admin/housekeeping-tasks` in `Backend/src/main/java/com/homestay/controllers/AdminHousekeepingController.java`
- [X] T026 [P] [US2] Implement `listManager()` and `listAdmin()` in `Frontend/src/api/housekeepingApi.ts`
- [X] T027 [US2] Create `HousekeepingTasksPage.tsx` kanban board (Pending / In Progress / Done columns) in `Frontend/src/pages/manager/HousekeepingTasksPage.tsx`
- [ ] T028 [US2] Create `HousekeepingTaskCard.tsx` showing room, assignee, status in `Frontend/src/components/housekeeping/HousekeepingTaskCard.tsx`
- [X] T029 [US2] Wire property context + load tasks on `HousekeepingTasksPage.tsx`
- [X] T030 [US2] Add empty state when no tasks on `HousekeepingTasksPage.tsx`
- [X] T031 [US2] Register `/manager/housekeeping` with `ProtectedRoute role="MANAGER"` in `Frontend/src/App.tsx`
- [X] T032 [US2] Add **Housekeeping** nav link in `Frontend/src/layouts/ManagerLayout.tsx`

**Checkpoint**: US2 testable — Manager SCR-40 board via `quickstart.md` list curl

---

## Phase 5: User Story 3 — Manager gán Employee / tạo task thủ công (Priority: P1)

**Goal**: Assign employee to task; manual create with optional assignee

**Independent Test**: PATCH assign succeeds for same-property employee; POST create works; wrong property employee 400

### Implementation

- [X] T033 [US3] Implement `createManual(roomId, assigneeId, managerId)` with property scope + FR-20 assignee validation in `HousekeepingTaskService.java`
- [X] T034 [US3] Implement `assignEmployee(taskId, employeeId, managerId)` — reject terminal tasks, validate employee property in `HousekeepingTaskService.java`
- [X] T035 [US3] Add `POST /api/v1/manager/housekeeping-tasks` in `ManagerHousekeepingController.java`
- [X] T036 [US3] Add `PATCH /api/v1/manager/housekeeping-tasks/{id}/assign` in `ManagerHousekeepingController.java`
- [X] T037 [P] [US3] Add `create()` and `assign()` to `Frontend/src/api/housekeepingApi.ts`
- [X] T038 [US3] Create `AssignHousekeepingDrawer.tsx` with employee picker (FR-20 employees at property) in `Frontend/src/components/housekeeping/AssignHousekeepingDrawer.tsx`
- [X] T039 [US3] Wire assign flow from task card click → drawer → reload board on `HousekeepingTasksPage.tsx`
- [X] T040 [US3] Add **Create Task** button + room/assignee form on `HousekeepingTasksPage.tsx`

**Checkpoint**: US3 testable — assign and manual create end-to-end

---

## Phase 6: User Story 4 — Employee bắt đầu dọn phòng (Priority: P1)

**Goal**: Employee starts task → IN_PROGRESS + room Cleaning In Progress

**Independent Test**: Employee PATCH IN_PROGRESS on assigned task; room sync; non-assignee 403

### Implementation

- [X] T041 [US4] Implement `updateStatus(taskId, status, employeeId)` — validate assignee, PENDING→IN_PROGRESS only, set `startedAt` in `HousekeepingTaskService.java`
- [X] T042 [US4] Sync room to `CLEANING_IN_PROGRESS` on IN_PROGRESS in `HousekeepingTaskService.syncRoomStatusForTask`
- [X] T043 [US4] Create `EmployeeHousekeepingController.java` with `GET /api/v1/employee/housekeeping-tasks` in `Backend/src/main/java/com/homestay/controllers/EmployeeHousekeepingController.java`
- [X] T044 [US4] Add `PATCH /api/v1/employee/housekeeping-tasks/{id}/status` in `EmployeeHousekeepingController.java`
- [X] T045 [P] [US4] Add `listEmployee()` and `updateStatus()` to `Frontend/src/api/housekeepingApi.ts`
- [X] T046 [US4] Create `HousekeepingWorkspacePage.tsx` touch-friendly task list in `Frontend/src/pages/employee/HousekeepingWorkspacePage.tsx`
- [X] T047 [US4] Add **Start** button calling `updateStatus(id, 'IN_PROGRESS')` on `HousekeepingWorkspacePage.tsx`
- [X] T048 [US4] Register `/employee/housekeeping` with `ProtectedRoute role="EMPLOYEE"` in `Frontend/src/App.tsx`

**Checkpoint**: US4 testable — employee start flow on SCR-60

---

## Phase 7: User Story 5 — Employee hoàn thành + chặn bypass Available (Priority: P1)

**Goal**: Complete task → room Available; block Manager manual Available bypass

**Independent Test**: Employee COMPLETED → room Available; Manager manual Available with open task rejected

### Implementation

- [X] T049 [US5] Extend `updateStatus` for IN_PROGRESS→COMPLETED — set `completedAt`, reject PENDING→COMPLETED skip in `HousekeepingTaskService.java`
- [X] T050 [US5] Sync room to `AVAILABLE` on COMPLETED in `HousekeepingTaskService.syncRoomStatusForTask`
- [X] T051 [US5] Implement `RoomStatusGuardService.assertCanSetAvailable(roomId)` — query open PENDING/IN_PROGRESS tasks in `RoomStatusGuardService.java`
- [X] T052 [US5] Call `assertCanSetAvailable` from FR-08 `RoomService.updateStatus` when target is AVAILABLE in `RoomService.java`
- [X] T053 [US5] Add **Finish** button calling `updateStatus(id, 'COMPLETED')` on `HousekeepingWorkspacePage.tsx`
- [X] T054 [US5] Show completed tasks in Done column on `HousekeepingTasksPage.tsx` after employee finish

**Checkpoint**: US5 testable — complete flow + bypass guard

---

## Phase 8: User Story 6 — Manager hủy tác vụ / lịch sử (Priority: P2)

**Goal**: Cancel Pending/In Progress tasks; filter completed history

**Independent Test**: Manager cancel → CANCELLED; room safe state; history filter shows completed tasks

### Implementation

- [X] T055 [US6] Implement `cancel(taskId, note, managerId)` — PENDING|IN_PROGRESS→CANCELLED, room→PENDING_CLEANING if was IN_PROGRESS in `HousekeepingTaskService.java`
- [X] T056 [US6] Add `PATCH /api/v1/manager/housekeeping-tasks/{id}/cancel` in `ManagerHousekeepingController.java`
- [X] T057 [P] [US6] Add `cancel(id, note)` to `Frontend/src/api/housekeepingApi.ts`
- [X] T058 [US6] Add cancel action with confirmation + note on `HousekeepingTasksPage.tsx`
- [X] T059 [US6] Add history filter `fromDate`/`toDate` + status=COMPLETED on `HousekeepingTasksPage.tsx` and wire to API
- [X] T060 [US6] Reject cancel on COMPLETED tasks in `HousekeepingTaskService.java`

**Checkpoint**: US6 testable — cancel + history filter

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Tests, audit, quickstart validation

- [X] T061 [P] Unit test status transitions and idempotent `onBookingCheckedOut` in `Backend/src/test/java/com/homestay/unit/HousekeepingTaskServiceTest.java`
- [X] T062 [P] Unit test `RoomStatusGuardService` blocks Available with open task in `HousekeepingTaskServiceTest.java`
- [ ] T063 Integration test Manager list/assign + Employee status + RBAC in `Backend/src/test/java/com/homestay/integration/HousekeepingControllerIT.java`
- [ ] T064 [P] Assert cross-property Manager 403 and non-assignee Employee 403 in `HousekeepingControllerIT.java`
- [ ] T065 [P] Assert skip PENDING→COMPLETED returns 400 in `HousekeepingControllerIT.java`
- [ ] T066 Run curl smoke tests in `specs/021-housekeeping-management/quickstart.md` and fix gaps
- [ ] T067 [P] Log `HOUSEKEEPING_CREATED`, `HOUSEKEEPING_ASSIGNED`, `HOUSEKEEPING_STATUS_CHANGED`, `HOUSEKEEPING_CANCELLED` via ActivityLogService in `HousekeepingTaskService.java`
- [X] T068 [P] Add Employee nav link to `/employee/housekeeping` in `Frontend/src/layouts/EmployeeLayout.tsx` (create layout if missing)

---


---

## Closeout (2026-07-23 align-to-audit)

- [X] C001 FR-012 Admin read-only GET /api/v1/admin/housekeeping-tasks + AdminHousekeepingPage
- [X] C002 Idempotency existsByBooking_Id on auto-create
- [X] C003 HousekeepingTaskServiceTest (create/idempotent/assign/cancel/admin)
- [X] C004 Redirect /manager/housekeeping → tasks; Cancelled kanban column + filter

Deferred: T003 Flyway V037 unique index; T012 PATCH status DTO (start/finish used); T028 separate TaskCard file.


## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: After FR-01, FR-06, FR-08, FR-20 blockers
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **US1 (P1)**: After Foundational — **MVP** auto-create hook
- **US2 (P1)**: After Foundational — list UI (can parallel US1 backend)
- **US3 (P1)**: After US2 (board page exists)
- **US4 (P1)**: After US3 (tasks can be assigned)
- **US5 (P1)**: After US4 (start before complete)
- **US6 (P2)**: After US2–US5 stable
- **Polish (Phase 9)**: After desired user stories

### User Story Dependencies

```text
Foundational → US1 (auto-create hook) — MVP backend
            → US2 (Manager board + Admin read)
            → US3 (assign + create)
            → US4 (employee start)
            → US5 (employee complete + bypass guard)
            → US6 (cancel + history)
```

### Parallel Opportunities

**Phase 1**: T004, T005 [P]  
**Phase 2** (after T014): T006–T013, T017 all [P]  
**US1 + US2**: T023–T025 backend parallel with T018–T020 after Foundational  
**US3**: T037 parallel with T033–T036  
**US4**: T045 parallel with T041–T044  
**Polish**: T061, T062, T064, T065, T067, T068 all [P]

### Parallel Example: User Story 2

```bash
T023–T025 Backend list + Manager/Admin controllers
T026 housekeepingApi list methods (parallel)
T027–T032 HousekeepingTasksPage + route + nav
```

### Parallel Example: Foundational

```bash
T006–T013 Entity, enum, repo, DTOs (all parallel)
T014–T015 Service skeletons
T016 SecurityConfig
T017 housekeepingApi.ts skeleton
```

---

## Implementation Strategy

### MVP First (User Story 1 + US2 list)

1. Complete Phase 1: Setup (T001–T005)
2. Complete Phase 2: Foundational (T006–T017)
3. Complete Phase 3: User Story 1 (T018–T022) — auto-create hook
4. Complete Phase 4: User Story 2 (T023–T032) — Manager board
5. **STOP and VALIDATE**: Checkout creates task visible on SCR-40

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 → Auto task on checkout (MVP backend)
3. US2 → Manager board (MVP UI)
4. US3 → Assign + manual create
5. US4 → Employee start
6. US5 → Employee complete + bypass guard
7. US6 → Cancel + history
8. Polish → Tests + quickstart

### Parallel Team Strategy

With multiple developers after Foundational:

- **Developer A**: US1 + US2 (auto-create + Manager board)
- **Developer B**: US3 + US4 (assign + employee start)
- **Developer C**: US5 + US6 (complete + cancel/guard)

---

## Notes

- FR-04 checkout hook is integration point — stub with direct service call in tests if FR-04 incomplete
- Employee layout may not exist — create minimal `EmployeeLayout.tsx` in T068
- Strict transitions enforced in service — no PENDING→COMPLETED shortcut
- Admin has GET-only — no assign/cancel on admin routes
- FR-22 Dashboard consumes counts — out of scope; expose list endpoint only
