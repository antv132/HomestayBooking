# Tasks: FR-22 Employee Dashboard

**Input**: Design documents from `specs/022-employee-dashboard/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/employee-dashboard-api.yaml, quickstart.md

**Phụ thuộc**: FR-01 (EMPLOYEE JWT + fullName); FR-20 (employee property assignments); FR-21 (housekeeping_tasks); FR-13 (maintenance_tickets); FR-23/FR-04 (room_inspections + booking check_out). **Ranh giới**: FR-22 owns composite `GET /api/v1/employee/dashboard` + SCR-59 UI; không bảng mới; không status updates (SCR-60/61/62).

**Tests**: Không có phase test riêng per-story. Unit + integration trong Phase Polish per plan Phase F.

**Organization**: Tasks grouped by user story (US1–US6) for independent delivery.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no blocking deps)
- **[Story]**: US1–US6 maps to spec.md user stories

## Path Conventions

- **Backend**: `Backend/src/main/java/com/homestay/`
- **Backend tests**: `Backend/src/test/java/com/homestay/`
- **Frontend**: `Frontend/src/`
- **Migrations**: `Backend/src/main/resources/db/migration/`
- **Contract**: `specs/022-employee-dashboard/contracts/employee-dashboard-api.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Auth/property blockers, optional indexes, dev proxy

- [X] T001 Verify FR-01 EMPLOYEE JWT auth per `specs/001-user-auth/quickstart.md` (blocker)
- [X] T002 Verify FR-20 property assignments, FR-21 housekeeping, FR-13 maintenance, FR-23/FR-04 room_inspections seed data per respective quickstarts (blocker)
- [X] T003 [P] Create optional Flyway `Backend/src/main/resources/db/migration/V038__employee_dashboard_indexes_fr22.sql` per `data-model.md`
- [X] T004 [P] Confirm Vite proxy `/api/v1` → backend in `Frontend/vite.config.ts`
- [X] T005 [P] Add dev seed SQL comments in V038 or README block in `specs/022-employee-dashboard/quickstart.md` — sample employee with 3 HK + 2 MT + 1 inspection tasks

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Dashboard DTOs, repository query extensions, service skeleton, controller, security — MUST complete before user story phases

**⚠️ CRITICAL**: No user story work until this phase complete

- [X] T006 [P] Create `HousekeepingTaskSummaryDto.java` in `Backend/src/main/java/com/homestay/dtos/dashboard/HousekeepingTaskSummaryDto.java` per `contracts/employee-dashboard-api.yaml`
- [X] T007 [P] Create `MaintenanceTicketSummaryDto.java` in `Backend/src/main/java/com/homestay/dtos/dashboard/MaintenanceTicketSummaryDto.java`
- [X] T008 [P] Create `RoomInspectionSummaryDto.java` in `Backend/src/main/java/com/homestay/dtos/dashboard/RoomInspectionSummaryDto.java`
- [X] T009 [P] Create `EmployeeTaskSummaryDto.java` in `Backend/src/main/java/com/homestay/dtos/dashboard/EmployeeTaskSummaryDto.java`
- [X] T010 [P] Create `AwaitingSummaryDto.java` and `CompletedTodaySummaryDto.java` in `Backend/src/main/java/com/homestay/dtos/dashboard/AwaitingSummaryDto.java`
- [X] T011 [P] Create `EmployeeDashboardResponse.java` in `Backend/src/main/java/com/homestay/dtos/dashboard/EmployeeDashboardResponse.java` — aggregate all dashboard fields
- [X] T012 [P] Add dashboard count/preview query methods to `HousekeepingTaskRepository.java` in `Backend/src/main/java/com/homestay/repositories/HousekeepingTaskRepository.java` per `data-model.md`
- [X] T013 [P] Add dashboard count/preview query methods to `MaintenanceTicketRepository.java` in `Backend/src/main/java/com/homestay/repositories/MaintenanceTicketRepository.java`
- [X] T014 [P] Add dashboard count/preview query methods to `RoomInspectionRepository.java` in `Backend/src/main/java/com/homestay/repositories/RoomInspectionRepository.java`
- [X] T015 Create `EmployeeDashboardService.java` skeleton in `Backend/src/main/java/com/homestay/services/EmployeeDashboardService.java` — inject repos + `EmployeePropertyAssignmentRepository`; `resolveAssignedPropertyIds(employeeId)`
- [X] T016 Create `EmployeeDashboardController.java` skeleton in `Backend/src/main/java/com/homestay/controllers/EmployeeDashboardController.java`
- [X] T017 Register `hasRole('EMPLOYEE')` for `/api/v1/employee/dashboard` and `/api/v1/employee/kpis` in `Backend/src/main/java/com/homestay/configs/SecurityConfig.java`
- [X] T018 [P] Create `employeeDashboardApi.ts` with `EmployeeDashboardData` types in `Frontend/src/api/employeeDashboardApi.ts` per contract

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — KPI tổng quan và lời chào (Priority: P1) 🎯 MVP

**Goal**: Greeting + 3 KPI counts + action cards navigating to SCR-60/61/62

**Independent Test**: Employee login → Dashboard shows fullName + pendingHousekeeping/Maintenance/Inspections matching seed data; non-Employee 403

### Implementation

- [X] T019 [US1] Implement `countPendingHousekeeping`, `countPendingMaintenance`, `countPendingInspections` in `EmployeeDashboardService.java` per `research.md` §3
- [X] T020 [US1] Implement `getDashboard(employeeId)` returning KPI fields + `fullName` from user profile in `EmployeeDashboardService.java`
- [X] T021 [US1] Add `GET /api/v1/employee/dashboard` in `EmployeeDashboardController.java` — resolve employeeId from JWT
- [X] T022 [US1] Add `GET /api/v1/employee/kpis` alias returning KPI subset in `EmployeeDashboardController.java`
- [X] T023 [P] [US1] Implement `fetchDashboard()` and `fetchKpis()` in `Frontend/src/api/employeeDashboardApi.ts`
- [X] T024 [US1] Create `EmployeeLayout.tsx` with mobile nav in `Frontend/src/layouts/EmployeeLayout.tsx`
- [X] T025 [US1] Create `EmployeeDashboardPage.tsx` shell with greeting + 3 action cards in `Frontend/src/pages/employee/EmployeeDashboardPage.tsx`
- [X] T026 [US1] Wire KPI counts to action cards ("X pending tasks") with `min-h-[100px]` touch-friendly styling on `EmployeeDashboardPage.tsx`
- [X] T027 [US1] Register `/employee/dashboard` with `ProtectedRoute role="EMPLOYEE"` in `Frontend/src/App.tsx`
- [X] T028 [US1] Wire action card navigation to `/employee/housekeeping`, `/employee/maintenance`, `/employee/inspections` on `EmployeeDashboardPage.tsx`

**Checkpoint**: US1 MVP — KPI dashboard testable via `quickstart.md` curl

---

## Phase 4: User Story 2 — Danh sách Housekeeping được gán (Priority: P1)

**Goal**: Preview list max 5 assigned housekeeping tasks (Pending + In Progress)

**Independent Test**: Employee with 3 HK tasks → dashboard `housekeepingTasks` length 3 with room + status; empty state when none

### Implementation

- [X] T029 [US2] Implement `listHousekeepingPreview(employeeId, propertyIds, limit=5)` in `EmployeeDashboardService.java` — PENDING first, `created_at ASC`
- [X] T030 [US2] Include `housekeepingTasks` in `getDashboard()` response in `EmployeeDashboardService.java`
- [X] T031 [US2] Add Housekeeping preview section to `EmployeeDashboardPage.tsx` — room, property, status badge
- [X] T032 [US2] Add empty state "Không có tác vụ dọn phòng" when list empty on `EmployeeDashboardPage.tsx`
- [X] T033 [US2] Add "Xem tất cả" link to `/employee/housekeeping` on `EmployeeDashboardPage.tsx`
- [X] T034 [US2] Show `propertyName` on each item when employee assigned to multiple properties on `EmployeeDashboardPage.tsx`

**Checkpoint**: US2 testable — housekeeping preview list on SCR-59

---

## Phase 5: User Story 3 — Danh sách Maintenance được gán (Priority: P1)

**Goal**: Preview list max 5 assigned maintenance tickets (Assigned + In Progress)

**Independent Test**: Employee with Assigned ticket → `maintenanceTickets` preview shows title + room; click Maintenance card → SCR-61

### Implementation

- [X] T035 [US3] Implement `listMaintenancePreview(employeeId, propertyIds, limit=5)` in `EmployeeDashboardService.java`
- [X] T036 [US3] Include `maintenanceTickets` in `getDashboard()` response in `EmployeeDashboardService.java`
- [X] T037 [US3] Add Maintenance preview section to `EmployeeDashboardPage.tsx` — title, room, status badge
- [X] T038 [US3] Add empty state when no maintenance tickets on `EmployeeDashboardPage.tsx`
- [X] T039 [US3] Add "Xem tất cả" link to `/employee/maintenance` on `EmployeeDashboardPage.tsx`

**Checkpoint**: US3 testable — maintenance preview on SCR-59

---

## Phase 6: User Story 4 — Tác vụ trong ngày hôm nay (Priority: P1)

**Goal**: Unified `todayTasks` section (max 10) with type labels

**Independent Test**: Inspection with booking check_out today → appears in `todayTasks` with type INSPECTION

### Implementation

- [X] T040 [US4] Implement `listTodayTasks(employeeId, propertyIds, limit=10)` — union HK/MT/RI per `research.md` §5 with `Asia/Ho_Chi_Minh` today filter in `EmployeeDashboardService.java`
- [X] T041 [US4] Include `todayTasks` in `getDashboard()` response in `EmployeeDashboardService.java`
- [X] T042 [US4] Add section **Tác vụ hôm nay** to `EmployeeDashboardPage.tsx` with type labels (Housekeeping / Maintenance / Inspection)
- [X] T043 [US4] Sort display: inspections with checkout today first on `EmployeeDashboardPage.tsx`
- [X] T044 [US4] Add empty state "Không có tác vụ hôm nay" on `EmployeeDashboardPage.tsx`

**Checkpoint**: US4 testable — today section with checkout-driven inspection

---

## Phase 7: User Story 5 — Đang chờ vs đã hoàn thành hôm nay (Priority: P1)

**Goal**: `awaiting` and `completedToday` summaries with counts + previews (max 3 per type)

**Independent Test**: Employee completes HK today → `completedToday.housekeepingCount` increments; yesterday completions excluded

### Implementation

- [X] T045 [US5] Implement `buildAwaitingSummary(employeeId, propertyIds)` with counts + previews max 3/type in `EmployeeDashboardService.java`
- [X] T046 [US5] Implement `buildCompletedTodaySummary(employeeId, propertyIds)` — HK COMPLETED, MT RESOLVED, RI PASSED today in `EmployeeDashboardService.java`
- [X] T047 [US5] Include `awaiting` and `completedToday` in `getDashboard()` response in `EmployeeDashboardService.java`
- [X] T048 [US5] Add section **Đang chờ xử lý** with counts + preview list on `EmployeeDashboardPage.tsx`
- [X] T049 [US5] Add section **Đã hoàn thành hôm nay** with counts + preview list on `EmployeeDashboardPage.tsx`
- [X] T050 [US5] Add workspace deep links from completed/awaiting previews on `EmployeeDashboardPage.tsx`

**Checkpoint**: US5 testable — awaiting vs completed today sections

---

## Phase 8: User Story 6 — Danh sách Room Inspection được gán (Priority: P1)

**Goal**: Preview list max 5 inspections (Pending + In Progress) with check-out date

**Independent Test**: Inspection Pending with `inspected_by` = employee → `inspections` preview; property scope enforced

### Implementation

- [X] T051 [US6] Implement `listInspectionPreview(employeeId, propertyIds, limit=5)` — join booking for `checkOutDate` in `EmployeeDashboardService.java`
- [X] T052 [US6] Include `inspections` in `getDashboard()` response in `EmployeeDashboardService.java`
- [X] T053 [US6] Add Inspection preview section to `EmployeeDashboardPage.tsx` — room, check-out date, status
- [X] T054 [US6] Add empty state when no inspections on `EmployeeDashboardPage.tsx`
- [X] T055 [US6] Add "Xem tất cả" link to `/employee/inspections` on `EmployeeDashboardPage.tsx`
- [X] T056 [US6] Handle `noPropertyAssignment=true` — show friendly message "Liên hệ Manager để được gán Property" on `EmployeeDashboardPage.tsx`

**Checkpoint**: US6 testable — inspection preview + no-property empty state

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Tests, quickstart validation, read-only guard

- [X] T057 [P] Unit test KPI counts match repository data in `Backend/src/test/java/com/homestay/unit/EmployeeDashboardServiceTest.java`
- [X] T058 [P] Unit test today filter and completed-today scope in `EmployeeDashboardServiceTest.java`
- [ ] T059 Integration test dashboard RBAC + employee data isolation in `Backend/src/test/java/com/homestay/integration/EmployeeDashboardControllerIT.java`
- [ ] T060 [P] Assert Manager/Customer GET dashboard returns 403 in `EmployeeDashboardControllerIT.java`
- [X] T061 [P] Assert KPI counts >= preview list lengths (consistency) in `EmployeeDashboardServiceTest.java`
- [ ] T062 Run curl smoke tests in `specs/022-employee-dashboard/quickstart.md` and fix gaps
- [X] T063 [P] Verify SCR-59 has no Start/Finish/Pass mutation buttons — read-only only on `EmployeeDashboardPage.tsx`
- [X] T064 [P] Add default Employee post-login redirect to `/employee/dashboard` in `Frontend/src/App.tsx` or auth redirect handler

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: After FR-01, FR-20, FR-21, FR-13, FR-23 blockers
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **US1 (P1)**: After Foundational — **MVP** KPI + action cards
- **US2–US6 (P1)**: After US1 backend `getDashboard()` exists; UI sections can incrementally wire
- **Polish (Phase 9)**: After desired user stories

### User Story Dependencies

```text
Foundational → US1 (KPI + greeting + action cards) — MVP
            → US2 (housekeeping preview)
            → US3 (maintenance preview)
            → US4 (today tasks)
            → US5 (awaiting vs completed today)
            → US6 (inspection preview + no-property state)
```

US2–US6 backend methods can be implemented in parallel after Foundational; each extends `getDashboard()` response.

### Parallel Opportunities

**Phase 1**: T003, T004, T005 [P]  
**Phase 2** (after T015): T006–T014, T018 all [P]  
**US1**: T023 parallel with T019–T022  
**US2 + US3**: T029–T030 parallel with T035–T036 after US1 service base  
**Polish**: T057, T058, T060, T061, T063, T064 all [P]

### Parallel Example: User Story 1

```bash
T019–T022 Backend KPI counts + controller endpoints
T023 employeeDashboardApi.ts (parallel)
T024–T028 EmployeeLayout + EmployeeDashboardPage + route
```

### Parallel Example: Foundational

```bash
T006–T011 All DTOs (parallel)
T012–T014 Repository query methods (parallel)
T015–T017 Service + controller + security
T018 API client types
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Complete Phase 1: Setup (T001–T005)
2. Complete Phase 2: Foundational (T006–T018)
3. Complete Phase 3: User Story 1 (T019–T028)
4. **STOP and VALIDATE**: Employee sees KPI dashboard via `quickstart.md` curl + UI

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 → KPI + action cards (MVP)
3. US2 → Housekeeping preview
4. US3 → Maintenance preview
5. US4 → Today tasks
6. US5 → Awaiting vs completed
7. US6 → Inspection preview + no-property state
8. Polish → Tests + quickstart

### Parallel Team Strategy

With multiple developers after Foundational:

- **Developer A**: US1 + US2 (KPI shell + housekeeping)
- **Developer B**: US3 + US4 (maintenance + today)
- **Developer C**: US5 + US6 (awaiting/completed + inspections)

---

## Notes

- `GET /api/v1/employee/kpis` is subset alias; primary contract is `/employee/dashboard`
- KPI `pendingInspections` extends original api-spec (housekeeping + maintenance only)
- All queries scoped via FR-20 `employee_property_assignments`
- Timezone `Asia/Ho_Chi_Minh` for all "today" filters
- SCR-60/61/62 routes may 404 until FR-21/13/23 implemented — action cards still navigate
- No audit log for read-only dashboard v1
