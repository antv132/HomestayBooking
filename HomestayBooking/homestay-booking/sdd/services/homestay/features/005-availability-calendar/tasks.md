# Tasks: FR-05 Availability Calendar

**Input**: Design documents from `specs/005-availability-calendar/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/availability-api.yaml, quickstart.md

**Phụ thuộc**: FR-03 (SCR-09 UI, base availability routes); FR-04 (booking statuses); FR-21 (housekeeping statuses); FR-06/08 (Room/Property). **Ranh giới**: WebSocket push (FR-15).

**Tests**: Không có phase test riêng per-story. Unit + integration trong Phase Polish per plan Phase K.

**Organization**: Tasks grouped by user story (US1–US5) for independent delivery.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no blocking deps)
- **[Story]**: US1–US5 maps to spec.md user stories

## Path Conventions

- **Backend**: `Backend/src/main/java/com/homestay/`
- **Backend tests**: `Backend/src/test/java/com/homestay/`
- **Frontend**: `Frontend/src/`
- **Migrations**: `Backend/src/main/resources/db/migration/`
- **Contract**: `specs/005-availability-calendar/contracts/availability-api.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Migration, seed data for calendar scenarios

- [X] T001 Verify backend Spring Boot scaffold (or FR-01 setup) per `specs/001-user-auth/plan.md`
- [X] T002 [P] Confirm Vite proxy `/api/v1` → backend in `Frontend/vite.config.ts`
- [X] T003 Create Flyway `Backend/src/main/resources/db/migration/V030__room_status_blocks.sql` per `data-model.md`
- [X] T004 [P] Create seed `Backend/src/main/resources/db/migration/V031__calendar_test_seed.sql` — sample bookings (PENDING_DEPOSIT, CONFIRMED, CHECKED_IN) + one maintenance block for dev/quickstart

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Enums, entity, resolver, DTOs, core services — MUST complete before user story phases

**⚠️ CRITICAL**: No user story work until this phase complete

- [X] T005 [P] Create `RoomCalendarStatus.java` enum (8 values) in `Backend/src/main/java/com/homestay/enums/RoomCalendarStatus.java`
- [X] T006 [P] Create `RoomStatusBlock.java` entity in `Backend/src/main/java/com/homestay/entities/RoomStatusBlock.java`
- [X] T007 [P] Create `RoomStatusBlockRepository.java` in `Backend/src/main/java/com/homestay/repositories/RoomStatusBlockRepository.java`
- [X] T008 Create `CalendarStatusResolver.java` in `Backend/src/main/java/com/homestay/services/CalendarStatusResolver.java` — priority merge per spec Assumptions
- [X] T009 Create `CalendarStatusService.java` skeleton in `Backend/src/main/java/com/homestay/services/CalendarStatusService.java` — inject BookingRepository, RoomStatusBlockRepository, HousekeepingTaskRepository, RoomRepository
- [X] T010 [P] Create calendar DTOs in `Backend/src/main/java/com/homestay/dtos/calendar/` — `CalendarDayResponse`, `RoomCalendarResponse`, `UpdateRoomStatusRequest`, `RoomStatusBlockResponse` per contracts/availability-api.yaml
- [X] T011 Create `HousekeepingGateService.java` in `Backend/src/main/java/com/homestay/services/HousekeepingGateService.java` — `canSetAvailable(roomId)`
- [X] T012 Register public calendar paths `permitAll` in `Backend/src/main/java/com/homestay/configs/SecurityConfig.java` — `GET /api/v1/rooms/*/availability`, `GET /api/v1/rooms/*/calendar`
- [X] T013 [P] Add `CALENDAR_STATUS_META` map (label, color, bookable) in `Frontend/src/utils/roomCalendar.ts`
- [X] T014 [P] Migrate calendar paths in `Frontend/src/api/roomsApi.ts` — `/api/v1/rooms/{id}/availability`, `/api/v1/rooms/{id}/calendar`, `/api/v1/manager/rooms/{id}/calendar`, `/api/v1/manager/rooms/{id}/status`

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Khách xem lịch 8 trạng thái (Priority: P1) 🎯 MVP

**Goal**: SCR-09 — month grid with 8 statuses, date selection on bookable days only

**Independent Test**: `GET /api/v1/rooms/{id}/availability?month=7&year=2026` → `days[]` with status + bookable; UI legend 8 colors; select Available range → navigate to room detail with dates

### Implementation

- [X] T015 [US1] Implement `buildMonthCalendar(roomId, month, year)` in `CalendarStatusService.java` — aggregate bookings, blocks, room.status, HK per data-model.md algorithm
- [X] T016 [US1] Map booking statuses → calendar status (PENDING_DEPOSIT, RESERVED, OCCUPIED) in `CalendarStatusService.java` per research.md #4
- [X] T017 [US1] Derive `bookedDates[]` from days where `bookable=false` for FR-03 backward compat in `CalendarStatusService.java`
- [X] T018 [US1] Create `RoomCalendarController.java` with `GET /api/v1/rooms/{id}/availability` — month mode (`month`, `year`) returning `RoomCalendarResponse`
- [X] T019 [US1] Add range mode `?checkIn&checkOut` delegating to `isBookable(roomId, range)` in `RoomCalendarController.java` (FR-03 compat)
- [X] T020 [US1] Keep `GET /api/v1/rooms/{id}/calendar` returning `bookedRanges` + `roomStatus` in `RoomCalendarController.java`
- [X] T021 [P] [US1] Add `fetchRoomMonthAvailability(roomId, month, year)` in `Frontend/src/api/roomsApi.ts` returning `days[]`
- [X] T022 [US1] Refactor `Frontend/src/pages/public/AvailabilityCalendarPage.tsx` — consume `days[]` + `CALENDAR_STATUS_META` for cell styling (replace occupied/pending-only logic)
- [X] T023 [P] [US1] Add 8-status legend component to `AvailabilityCalendarPage.tsx` per `docs/component-library.md` §3.8
- [X] T024 [US1] Restrict date click to `bookable===true` days only in `AvailabilityCalendarPage.tsx`
- [X] T025 [US1] Wire month navigation to refetch `fetchRoomMonthAvailability` in `AvailabilityCalendarPage.tsx`

**Checkpoint**: US1 MVP — SCR-09 public calendar with 8 statuses

---

## Phase 4: User Story 2 — Manager xem lịch phòng (Priority: P1)

**Goal**: Manager enriched calendar with booking/block refs, property-scoped

**Independent Test**: Manager GET `/manager/rooms/{id}/calendar?month&year` → days with optional bookingId; 403 for unassigned property

### Implementation

- [X] T026 [US2] Add `buildManagerMonthCalendar()` with `bookingId`, `blockId` on days in `CalendarStatusService.java`
- [X] T027 [US2] Add `GET /api/v1/manager/rooms/{id}/calendar` in `RoomCalendarController.java` with `@PropertyAccess` validation
- [X] T028 [US2] Strip sensitive guest data from public response — ensure public endpoint never returns bookingId in `RoomCalendarController.java`
- [X] T029 [P] [US2] Add manager calendar fetch in `Frontend/src/api/roomsApi.ts` — `fetchManagerRoomCalendar`
- [X] T030 [US2] Add calendar tab/section on `Frontend/src/pages/manager/RoomDetailMgmtPage.tsx` using shared calendar component with tooltips (booking ref)
- [X] T031 [P] [US2] Extract reusable `RoomAvailabilityCalendar.tsx` in `Frontend/src/components/rooms/RoomAvailabilityCalendar.tsx` from `AvailabilityCalendarPage.tsx` grid logic

**Checkpoint**: US2 testable — manager calendar view SCR-33 context

---

## Phase 5: User Story 3 — Manager cập nhật trạng thái thủ công (Priority: P1)

**Goal**: SCR-33 — Maintenance/Out Of Service blocks with date range; HK gate on Available

**Independent Test**: PATCH status with date range → calendar shows MAINTENANCE; overlap booking → 409; Available with open HK → 409

### Implementation

- [X] T032 [US3] Create `RoomStatusBlockService.java` in `Backend/src/main/java/com/homestay/services/RoomStatusBlockService.java` — create/deactivate blocks, validate no booking overlap
- [X] T033 [US3] Create `ManagerRoomStatusController.java` with `PATCH /api/v1/manager/rooms/{id}/status` in `Backend/src/main/java/com/homestay/controllers/ManagerRoomStatusController.java`
- [X] T034 [US3] On MAINTENANCE/OUT_OF_SERVICE: persist block + update `Room.status` if range includes today in `RoomStatusBlockService.java`
- [X] T035 [US3] On AVAILABLE: call `HousekeepingGateService.canSetAvailable()` before update in `ManagerRoomStatusController.java`
- [X] T036 [US3] Add `GET /api/v1/manager/rooms/{id}/status-blocks` list active blocks in `ManagerRoomStatusController.java`
- [X] T037 [P] [US3] Log `ROOM_STATUS_BLOCK_CREATED` to ActivityLog in `RoomStatusBlockService.java`
- [X] T038 [US3] Extend `UpdateRoomStatusPayload` in `Frontend/src/api/roomsApi.ts` — add `startDate`, `endDate`, `reason`, `OUT_OF_SERVICE`
- [X] T039 [US3] Update `Frontend/src/pages/manager/RoomStatusPage.tsx` — date range pickers + reason textarea per SCR-33 wireframe
- [X] T040 [US3] Add OUT_OF_SERVICE option and display API 409 HK/conflict errors in `RoomStatusPage.tsx`
- [X] T041 [US3] Migrate `updateRoomStatus` to `PATCH /api/v1/manager/rooms/{id}/status` in `Frontend/src/api/roomsApi.ts`

**Checkpoint**: US3 testable — manual status management SCR-33

---

## Phase 6: User Story 4 — Employee xem lịch read-only (Priority: P2)

**Goal**: Employee can GET manager calendar endpoint; no PATCH

**Independent Test**: Employee JWT → calendar 200 for assigned property; PATCH status → 403

### Implementation

- [X] T042 [US4] Allow `@PreAuthorize("hasAnyRole('MANAGER','EMPLOYEE','ADMIN')")` on manager calendar GET in `RoomCalendarController.java`
- [X] T043 [US4] Restrict PATCH status to MANAGER and ADMIN only in `ManagerRoomStatusController.java`
- [X] T044 [US4] Add Employee property assignment check (reuse `PropertyAccessValidator`) for calendar GET in `RoomCalendarController.java`
- [X] T045 [P] [US4] Hide status update form when role is EMPLOYEE on `Frontend/src/pages/manager/RoomStatusPage.tsx` (or route guard)

**Checkpoint**: US4 testable — employee read-only calendar

---

## Phase 7: User Story 5 — Ưu tiên trạng thái (Priority: P2)

**Goal**: Priority resolver correctness across booking + block + HK sources

**Independent Test**: Unit matrix — Occupied beats Maintenance; Pending Deposit beats Available; seed scenarios match API

### Implementation

- [X] T046 [US5] Implement booking→calendar mapping helper `BookingCalendarMapper.java` in `Backend/src/main/java/com/homestay/services/BookingCalendarMapper.java`
- [X] T047 [US5] Integrate HK room status into day sources in `CalendarStatusService.java` per research.md #5
- [X] T048 [US5] Refactor FR-03 `AvailabilityService.java` (if exists) to delegate range check to `CalendarStatusService.isBookable()` in `Backend/src/main/java/com/homestay/services/AvailabilityService.java`
- [X] T049 [US5] Document priority table in code comment on `CalendarStatusResolver.java` matching spec Assumptions

**Checkpoint**: US5 — consistent single status per day across all sources

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Tests, FR-012 refresh, quickstart, Admin scope

- [X] T050 [P] Unit test `CalendarStatusResolverTest.java` in `Backend/src/test/java/com/homestay/unit/CalendarStatusResolverTest.java` — full priority matrix
- [X] T051 Integration test `RoomCalendarControllerIT.java` in `Backend/src/test/java/com/homestay/integration/RoomCalendarControllerIT.java` — public month + manager block + 409 overlap
- [X] T052 [P] Add React Query `refetchInterval: 30000` on calendar fetches in `AvailabilityCalendarPage.tsx` and manager calendar view (FR-012 until FR-15)
- [X] T053 Allow ADMIN all-properties access on manager calendar + status PATCH in `PropertyAccessValidator.java`
- [X] T054 Run curl smoke tests in `specs/005-availability-calendar/quickstart.md` and fix gaps
- [X] T055 [P] Verify `Frontend/src/App.tsx` routes for `/rooms/:id/calendar` and manager room status page

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Start immediately
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **US1 (P1)**: After Foundational — MVP public calendar
- **US2 (P1)**: After US1 calendar service (extends same service)
- **US3 (P1)**: After Foundational; benefits from US1 calendar to verify blocks visually
- **US4 (P2)**: After US2 manager GET exists
- **US5 (P2)**: Resolver in Phase 2; integration tasks after US1–US3
- **Polish**: After desired stories

### User Story Dependencies

| Story | Depends on | Independent test |
|-------|------------|------------------|
| US1 | Foundational | Public month API + SCR-09 UI |
| US2 | US1 CalendarStatusService | Manager calendar GET |
| US3 | Foundational + US1 (verify blocks on calendar) | PATCH status + 409 rules |
| US4 | US2 manager GET | Employee read, no write |
| US5 | US1–US3 data paths | Priority resolver tests |

### Parallel Opportunities

- Phase 1: T002, T004 parallel
- Phase 2: T005–T007, T010, T013–T014 parallel
- US1: T021, T023 parallel after T018
- US2: T029, T031 parallel
- US3: T037 parallel with controller work
- US4: T045 parallel
- Polish: T050, T052, T055 parallel

---

## Parallel Example: User Story 1

```bash
# After T018 controller exists:
Task T021: roomsApi.ts fetchRoomMonthAvailability
Task T023: AvailabilityCalendarPage.tsx legend
Task T031: (optional early) extract RoomAvailabilityCalendar.tsx
```

---

## Parallel Example: Foundational Phase

```bash
T005 RoomCalendarStatus.java | T006 RoomStatusBlock.java | T007 RoomStatusBlockRepository.java
T013 roomCalendar.ts | T014 roomsApi.ts path migration
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 + Phase 2
2. Phase 3 (US1) — public 8-status calendar SCR-09
3. **STOP and VALIDATE**: quickstart month GET + UI legend
4. Demo date selection → room detail

### Incremental Delivery

1. Setup + Foundational
2. US1 → guest calendar MVP
3. US2 → manager view
4. US3 → SCR-33 manual blocks
5. US4 → employee read-only
6. US5 + Polish → priority tests + 30s refresh

### Suggested MVP Scope

**T001–T025** (Setup + Foundational + US1) — enriched SCR-09 calendar for Guest/Customer.

---

## Notes

- `CalendarStatusResolver` (T008) is shared by US1 and US5 — implement fully in Foundational
- Do not duplicate booking writes — calendar is read-only aggregation
- Guest responses MUST NOT include bookingId (privacy FR-002)
- Commit after each phase checkpoint
