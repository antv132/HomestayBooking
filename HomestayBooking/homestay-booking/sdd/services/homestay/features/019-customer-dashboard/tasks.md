# Tasks: FR-19 Customer Dashboard

**Input**: Design documents from `specs/019-customer-dashboard/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/customer-dashboard-api.yaml, quickstart.md

**Phụ thuộc**: FR-01 (CUSTOMER JWT); FR-04 (bookings); FR-12 (payments); FR-13 (maintenance tickets); FR-15 (notifications); FR-02 (greeting `fullName`). **Ranh giới**: FR-19 owns composite `GET /api/v1/customer/dashboard` + SCR-15 UI wire; không bảng mới; không CRUD; Damage Dispute alert P2 (FR-23).

**Tests**: Không có phase test riêng per-story. Unit + integration trong Phase Polish per plan.

**Organization**: Tasks grouped by user story (US1–US6) for independent delivery.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no blocking deps)
- **[Story]**: US1–US6 maps to spec.md user stories

## Path Conventions

- **Backend**: `Backend/src/main/java/com/homestay/`
- **Backend tests**: `Backend/src/test/java/com/homestay/`
- **Frontend**: `Frontend/src/`
- **Migrations**: `Backend/src/main/resources/db/migration/`
- **Contract**: `specs/019-customer-dashboard/contracts/customer-dashboard-api.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Auth blocker, dependency verification, optional indexes, dev proxy

- [ ] T001 Verify FR-01 CUSTOMER JWT auth per `specs/001-user-auth/quickstart.md` (blocker)
- [ ] T002 Verify FR-04/FR-12/FR-13/FR-15 repositories and seed data exist per respective quickstarts (blocker)
- [ ] T003 [P] Create optional Flyway `Backend/src/main/resources/db/migration/V035__customer_dashboard_indexes_fr19.sql` per `data-model.md`
- [ ] T004 [P] Confirm Vite proxy `/api/v1` → backend in `Frontend/vite.config.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Dashboard DTOs, repository query extensions, service skeleton, controller, security — MUST complete before user story phases

**⚠️ CRITICAL**: No user story work until this phase complete

- [ ] T005 [P] Create `UpcomingEventDto.java` in `Backend/src/main/java/com/homestay/dtos/dashboard/UpcomingEventDto.java` per `contracts/customer-dashboard-api.yaml`
- [ ] T006 [P] Create `PaymentSummaryDto.java` in `Backend/src/main/java/com/homestay/dtos/dashboard/PaymentSummaryDto.java`
- [ ] T007 [P] Create `NotificationSummaryDto.java` in `Backend/src/main/java/com/homestay/dtos/dashboard/NotificationSummaryDto.java`
- [ ] T008 [P] Create `DamageDisputeAlertDto.java` in `Backend/src/main/java/com/homestay/dtos/dashboard/DamageDisputeAlertDto.java` (P2 optional field)
- [ ] T009 [P] Create `CustomerDashboardResponse.java` in `Backend/src/main/java/com/homestay/dtos/dashboard/CustomerDashboardResponse.java` — aggregate all dashboard fields
- [ ] T010 [P] Add dashboard count/query methods to `BookingRepository.java` in `Backend/src/main/java/com/homestay/repositories/BookingRepository.java` per `data-model.md`
- [ ] T011 [P] Add dashboard count/query methods to `PaymentRepository.java` in `Backend/src/main/java/com/homestay/repositories/PaymentRepository.java`
- [ ] T012 [P] Add dashboard count/query methods to `MaintenanceTicketRepository.java` in `Backend/src/main/java/com/homestay/repositories/MaintenanceTicketRepository.java`
- [ ] T013 [P] Add dashboard count/query methods to `NotificationRepository.java` in `Backend/src/main/java/com/homestay/repositories/NotificationRepository.java`
- [ ] T014 Create `CustomerDashboardService.java` skeleton in `Backend/src/main/java/com/homestay/services/CustomerDashboardService.java` — inject repos; `getDashboard(customerId, userId)` returns empty/zero `CustomerDashboardResponse`
- [ ] T015 Create `CustomerDashboardController.java` with `GET /api/v1/customer/dashboard` in `Backend/src/main/java/com/homestay/controllers/CustomerDashboardController.java` — resolve customerId/userId from JWT; standard `{ success, message, data }` envelope
- [ ] T016 Register `hasRole('CUSTOMER')` for `GET /api/v1/customer/dashboard` and deny non-CUSTOMER in `Backend/src/main/java/com/homestay/configs/SecurityConfig.java`
- [ ] T017 [P] Migrate `fetchCustomerDashboard()` to `GET /api/v1/customer/dashboard` in `Frontend/src/api/customersApi.ts`
- [ ] T018 [P] Add optional `pendingDamageDispute` field to `CustomerDashboardData` interface in `Frontend/src/api/customersApi.ts`

**Checkpoint**: Foundation ready — endpoint returns zeroed payload; frontend calls new path

---

## Phase 3: User Story 1 — Customer xem tổng quan KPI (Priority: P1) 🎯 MVP

**Goal**: SCR-15 KPI row — active bookings, pending payments, open tickets, unread notifications scoped to logged-in Customer

**Independent Test**: Customer login → Dashboard shows 4 KPI counts matching source data; other Customer sees different counts; Manager/Guest denied

### Implementation

- [ ] T019 [US1] Implement `countActiveBookings(customerId)` — status CONFIRMED + CHECKED_IN in `CustomerDashboardService.java`
- [ ] T020 [US1] Implement `countPendingPayments(customerId)` — status PENDING + PENDING_VERIFICATION in `CustomerDashboardService.java`
- [ ] T021 [US1] Implement `countOpenTickets(customerId)` — status OPEN + IN_PROGRESS in `CustomerDashboardService.java`
- [ ] T022 [US1] Implement `countUnreadNotifications(userId)` in `CustomerDashboardService.java`
- [ ] T023 [US1] Wire KPI fields into `getDashboard()` response in `CustomerDashboardService.java`
- [ ] T024 [US1] Align main KPI row on `Frontend/src/pages/customer/CustomerDashboardPage.tsx` with spec FR-002 — active bookings, pending payments, open tickets, unread notifications (move from sidebar quick-stats if needed)
- [ ] T025 [US1] Highlight `pendingPayments` KPI with warning color when > 0 on `CustomerDashboardPage.tsx`
- [ ] T026 [US1] Verify zero-data Customer shows 0/— without error on `CustomerDashboardPage.tsx` (SC-005)
- [ ] T027 [US1] Verify `ProtectedRoute role="CUSTOMER"` on `/customer/dashboard` in `Frontend/src/App.tsx` — Manager/Admin redirected (SC-004)

**Checkpoint**: US1 MVP — KPI row testable via `quickstart.md` KPI curl

---

## Phase 4: User Story 2 — Customer xem sự kiện Check-in/Check-out sắp tới (Priority: P1)

**Goal**: Nearest upcoming check-in (CONFIRMED) and check-out (CHECKED_IN) event cards with countdown

**Independent Test**: Customer with Confirmed booking check-in in 3 days → event card shows room, property, date, daysUntil; click → booking detail

### Implementation

- [ ] T028 [US2] Implement `findUpcomingCheckIn(customerId)` — nearest CONFIRMED with `checkIn >= today` in `CustomerDashboardService.java`
- [ ] T029 [US2] Implement `findUpcomingCheckOut(customerId)` — nearest CHECKED_IN with `checkOut >= today` in `CustomerDashboardService.java`
- [ ] T030 [US2] Compute `daysUntil` using `Asia/Ho_Chi_Minh` timezone in `CustomerDashboardService.java`
- [ ] T031 [US2] Map `upcomingCheckIn` and `upcomingCheckOut` into `getDashboard()` response in `CustomerDashboardService.java`
- [ ] T032 [US2] Wire `EventCard` components to live `upcomingCheckIn`/`upcomingCheckOut` on `CustomerDashboardPage.tsx`
- [ ] T033 [US2] Add empty state "Không có lịch sắp tới" on `EventCard` when event is null on `CustomerDashboardPage.tsx`
- [ ] T034 [US2] Link event card click to `/customer/bookings/{bookingId}` on `CustomerDashboardPage.tsx` (SC-003)
- [ ] T035 [US2] Show warning badge when `daysUntil <= 3` on check-in `EventCard` on `CustomerDashboardPage.tsx`

**Checkpoint**: US2 testable — upcoming check-in/out cards per `quickstart.md`

---

## Phase 5: User Story 3 — Customer xem danh sách booking sắp tới (Priority: P1)

**Goal**: Upcoming bookings list (max 5) sorted by check-in with detail link and deposit CTA

**Independent Test**: List shows ≤5 upcoming bookings; PENDING_DEPOSIT shows pay CTA; empty state + Tìm phòng; Xem tất cả → SCR-17

### Implementation

- [ ] T036 [US3] Implement `listUpcomingBookings(customerId, limit=5)` — status PENDING_DEPOSIT, CONFIRMED, CHECKED_IN; `checkOut >= today`; ORDER BY checkIn ASC in `CustomerDashboardService.java`
- [ ] T037 [US3] Map bookings to `BookingSummaryResponse` shape (reuse FR-04 DTO/mapper) in `CustomerDashboardService.java`
- [ ] T038 [US3] Wire `upcomingBookings` list section to live API data on `CustomerDashboardPage.tsx`
- [ ] T039 [US3] Verify **Thanh toán cọc** link for `PENDING_DEPOSIT` status on `CustomerDashboardPage.tsx`
- [ ] T040 [US3] Verify empty state + **Tìm phòng** CTA links to `/rooms` on `CustomerDashboardPage.tsx`
- [ ] T041 [US3] Verify **Xem tất cả →** links to `/customer/bookings` on `CustomerDashboardPage.tsx`

**Checkpoint**: US3 testable — upcoming bookings list

---

## Phase 6: User Story 4 — Customer xem thông báo mới nhất (Priority: P1)

**Goal**: Recent notifications sidebar (max 5) with read/unread distinction and navigation

**Independent Test**: Dashboard shows 5 newest notifications; unread styled; click → detail; Tất cả → notifications list

### Implementation

- [ ] T042 [US4] Implement `listRecentNotifications(userId, limit=5)` — ORDER BY createdAt DESC in `CustomerDashboardService.java`
- [ ] T043 [US4] Map to `NotificationSummaryDto` in `CustomerDashboardService.java`
- [ ] T044 [US4] Wire `recentNotifications` sidebar to live API on `CustomerDashboardPage.tsx`
- [ ] T045 [US4] Verify unread highlight (border/background) for `isRead=false` on `CustomerDashboardPage.tsx`
- [ ] T046 [US4] Verify notification click navigates to `/customer/notifications/{id}` on `CustomerDashboardPage.tsx`
- [ ] T047 [US4] Verify **Tất cả →** links to `/customer/notifications` on `CustomerDashboardPage.tsx`
- [ ] T048 [US4] Verify empty state "Không có thông báo mới" on `CustomerDashboardPage.tsx`

**Checkpoint**: US4 testable — notifications sidebar

---

## Phase 7: User Story 5 — Customer xem thanh toán gần đây (Priority: P1)

**Goal**: Recent payments section (max 3) with type, amount, status, date — **UI section currently missing**

**Independent Test**: Dashboard shows 3 newest payments; Pending badge warning; Xem tất cả → payment history

### Implementation

- [ ] T049 [US5] Implement `listRecentPayments(customerId, limit=3)` — scoped via booking → customer; ORDER BY createdAt DESC in `CustomerDashboardService.java`
- [ ] T050 [US5] Map to `PaymentSummaryDto` in `CustomerDashboardService.java`
- [ ] T051 [US5] Add **Thanh toán gần đây** section (3-item list) to `Frontend/src/pages/customer/CustomerDashboardPage.tsx` below upcoming bookings or in main column
- [ ] T052 [US5] Display payment type, amount (₫ formatted), status badge, and date in recent payments section on `CustomerDashboardPage.tsx`
- [ ] T053 [US5] Show warning status badge for PENDING / PENDING_VERIFICATION payments on `CustomerDashboardPage.tsx`
- [ ] T054 [US5] Add **Xem tất cả →** link to `/customer/payments` in recent payments section on `CustomerDashboardPage.tsx`
- [ ] T055 [US5] Add empty state message when `recentPayments` is empty on `CustomerDashboardPage.tsx`

**Checkpoint**: US5 testable — recent payments section (SC-007)

---

## Phase 8: User Story 6 — Quick links và cảnh báo Damage Dispute (Priority: P2)

**Goal**: Verify quick action links; optional damage dispute alert banner when FR-23 pending dispute exists

**Independent Test**: Quick links navigate correctly; dispute banner shows only when `pendingDamageDispute` present

### Implementation

- [ ] T056 [US6] Verify quick links on `CustomerDashboardPage.tsx` — `/rooms`, `/customer/bookings`, `/customer/maintenance/create` per FR-009
- [ ] T057 [US6] Implement `findPendingDamageDispute(customerId)` in `CustomerDashboardService.java` — FR-23 `AWAITING_CUSTOMER_DISPUTE` status (P2; skip if FR-23 not ready)
- [ ] T058 [US6] Add `pendingDamageDispute` to `getDashboard()` response in `CustomerDashboardService.java`
- [ ] T059 [US6] Add dispute alert banner component with **Dispute** CTA on `CustomerDashboardPage.tsx` when `pendingDamageDispute` is non-null
- [ ] T060 [US6] Hide dispute banner when `pendingDamageDispute` is null on `CustomerDashboardPage.tsx`
- [ ] T061 [US6] Link Dispute CTA to FR-23 dispute route (e.g. `/customer/damage-reports/{id}/dispute`) on `CustomerDashboardPage.tsx`

**Checkpoint**: US6 testable — quick links + optional P2 banner

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Tests, quickstart validation, error handling, docs

- [ ] T062 [P] Unit test KPI count accuracy in `Backend/src/test/java/com/homestay/unit/CustomerDashboardServiceTest.java`
- [ ] T063 [P] Unit test `daysUntil` timezone and upcoming event selection in `CustomerDashboardServiceTest.java`
- [ ] T064 Integration test `GET /api/v1/customer/dashboard` RBAC + customer scope in `Backend/src/test/java/com/homestay/integration/CustomerDashboardControllerIT.java`
- [ ] T065 [P] Assert Customer A cannot see Customer B bookings in dashboard payload in `CustomerDashboardControllerIT.java`
- [ ] T066 [P] Assert Manager/Admin receive 403 on dashboard endpoint in `CustomerDashboardControllerIT.java`
- [ ] T067 Run curl smoke tests in `specs/019-customer-dashboard/quickstart.md` and fix gaps
- [ ] T068 Add friendly error + retry button on dashboard load failure in `CustomerDashboardPage.tsx`
- [ ] T069 [P] Update SCR-15 response example in `docs/api-spec-by-screen.md` to full `CustomerDashboardData` shape

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: After FR-01 + FR-04/12/13/15 blockers
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **US1 (P1)**: After Foundational — **MVP** KPI row
- **US2 (P1)**: After Foundational; shares `CustomerDashboardService.getDashboard()`
- **US3 (P1)**: After Foundational; same endpoint payload
- **US4 (P1)**: After Foundational; same endpoint payload
- **US5 (P1)**: After Foundational; adds missing UI section
- **US6 (P2)**: After US1–US5 stable; FR-23 optional for dispute banner
- **Polish (Phase 9)**: After desired user stories complete

### User Story Dependencies

```text
Foundational → US1 (KPI) — MVP
            → US2 (check-in/out events)
            → US3 (upcoming bookings)
            → US4 (notifications)
            → US5 (recent payments + new UI section)
            → US6 (quick links + dispute P2)
```

All US2–US5 extend the same `getDashboard()` method — implement backend fields sequentially or in one pass after Foundational, then verify frontend per story.

### Parallel Opportunities

**Phase 1**: T003, T004 [P]  
**Phase 2** (after T014): T005–T013, T017–T018 all [P]  
**US1**: T024–T027 frontend tasks parallel after T023  
**US2**: T032–T035 frontend parallel after T031  
**US5**: T051–T055 all frontend after T050  
**US6**: T056 parallel with T057–T058 backend  
**Polish**: T062, T063, T065, T066, T069 all [P]

### Parallel Example: User Story 1

```bash
T019–T023 Backend KPI counts in CustomerDashboardService
T024–T027 Frontend KPI row align + RBAC verify (parallel when API returns KPIs)
```

### Parallel Example: Foundational

```bash
T005–T009 DTOs (all parallel)
T010–T013 Repository query methods (all parallel)
T015 Controller + T016 Security (sequential after T014)
T017–T018 Frontend API migration (parallel)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T004)
2. Complete Phase 2: Foundational (T005–T018)
3. Complete Phase 3: User Story 1 (T019–T027)
4. **STOP and VALIDATE**: KPI counts per `quickstart.md` smoke test
5. Demo SCR-15 Dashboard with live KPI row

### Incremental Delivery

1. Setup + Foundational → endpoint returns zeroed payload
2. US1 → KPI row (MVP)
3. US2 → Check-in/out event cards
4. US3 → Upcoming bookings list
5. US4 → Notifications sidebar
6. US5 → Recent payments section (new UI)
7. US6 → Quick links verify + dispute banner P2
8. Polish → Tests + quickstart

### Parallel Team Strategy

With multiple developers after Foundational:

- **Developer A**: US1 + US2 (KPI + events backend + frontend)
- **Developer B**: US3 + US4 (bookings + notifications)
- **Developer C**: US5 + US6 (payments section + quick links/dispute)

---

## Notes

- Single composite endpoint — backend fields can be implemented in one service pass; frontend verified per story
- `CustomerDashboardPage.tsx` ~90% complete — US5 adds missing payments section; US1 may realign KPI row per spec
- No new database tables — optional V035 indexes only
- Damage dispute banner P2 — blocked until FR-23; stub `findPendingDamageDispute` returning null is acceptable for MVP
- Greeting uses `fullName` from `useAuthStore` (FR-02) — already wired
- Deprecate legacy `/api/customers/dashboard` — remove if exists in backend mock controllers

---

## Implement status (2026-07-23 gap-close)

| Fix | Status |
|-----|--------|
| `activeBookings` = CONFIRMED + CHECKED_IN only | Done (`countActiveByCustomerId`) |
| Pending deposit counted separately for pendingPayments KPI | Done (`countPendingDepositByCustomerId`) |
| Upcoming max 5 + `checkOutDate >= today` | Done |
| Recent payments max 3 | Done |
| KPI row = active / pendingPayments / openTickets / unread | Done (FE) |
| EventCard warning when `daysUntil ≤ 3` | Done |
| Timezone `Asia/Ho_Chi_Minh` | Done |
| Unit test `CustomerDashboardServiceTest` | Passed |
| Damage dispute banner (FR-011 P2) | Deferred |
| V035 indexes / curl UAT | Deferred — restart BE to pick up changes |