# Tasks: FR-16 Reporting

**Input**: Design documents from `specs/016-reporting/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/reporting-api.yaml, quickstart.md

**Phụ thuộc**: FR-01 (RBAC); FR-06 (Manager property scope); FR-07 (floor counts); FR-08 (rooms/status); FR-04 (bookings); FR-12 (PAID payments — revenue source); FR-09 (new customers KPI optional). **Ranh giới**: FR-16 read-only aggregates + report UI; no source entity CRUD; Employee/Customer excluded v1.

**Tests**: Không có phase test riêng per-story. Unit + integration trong Phase Polish per plan Phase L.

**Organization**: Tasks grouped by user story (US1–US5) for independent delivery.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no blocking deps)
- **[Story]**: US1–US5 maps to spec.md user stories

## Path Conventions

- **Backend**: `Backend/src/main/java/com/homestay/`
- **Backend tests**: `Backend/src/test/java/com/homestay/`
- **Frontend**: `Frontend/src/`
- **Migrations**: `Backend/src/main/resources/db/migration/`
- **Contract**: `specs/016-reporting/contracts/reporting-api.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify source data + optional reporting indexes — after FR-04/06/12

- [X] T001 Verify FR-12 `payments` table with **PAID** rows exists per `specs/012-payment-management/quickstart.md` (blocker for revenue)
- [X] T002 Verify FR-06 Manager **property assignment** seed exists per `specs/006-property-management/quickstart.md` (blocker for scope)
- [ ] T003 Create Flyway `Backend/src/main/resources/db/migration/V032__reporting_indexes_fr16.sql` — reporting indexes per `data-model.md`
- [X] T004 [P] Confirm Vite proxy `/api/v1` → backend in `Frontend/vite.config.ts`
- [ ] T005 [P] Add dev seed note or SQL in V032 comments — multi-month PAID payments across 2 properties for report demos

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Report DTOs, validators, repository, service/controller skeletons, security — MUST complete before user story phases

**⚠️ CRITICAL**: No user story work until this phase complete

- [X] T006 [P] Create report DTOs in `Backend/src/main/java/com/homestay/dtos/report/` — `PropertyKpisResponse`, `GlobalKpisResponse`, `RevenueReportResponse`, `OccupancyReportResponse`, `BookingTrendReportResponse`, `PropertyReportSummaryResponse` per `contracts/reporting-api.yaml`
- [ ] T007 [P] Create `ReportQueryRepository.java` in `Backend/src/main/java/com/homestay/repositories/ReportQueryRepository.java` — native aggregate query stubs
- [X] T008 Create `ReportPropertyScopeValidator.java` in `Backend/src/main/java/com/homestay/services/ReportPropertyScopeValidator.java` — reuse FR-06 assignment check
- [ ] T009 Create `ReportDateRangeValidator.java` in `Backend/src/main/java/com/homestay/validations/ReportDateRangeValidator.java` — `from`/`to`, max 366 days, `to >= from`
- [X] T010 [P] Create `RevenueReportService.java` skeleton in `Backend/src/main/java/com/homestay/services/RevenueReportService.java`
- [X] T011 [P] Create `OccupancyReportService.java` skeleton in `Backend/src/main/java/com/homestay/services/OccupancyReportService.java`
- [X] T012 [P] Create `BookingTrendReportService.java` skeleton in `Backend/src/main/java/com/homestay/services/BookingTrendReportService.java`
- [X] T013 [P] Create `PropertyKpisService.java` skeleton in `Backend/src/main/java/com/homestay/services/PropertyKpisService.java`
- [X] T014 [P] Create `GlobalKpisService.java` skeleton in `Backend/src/main/java/com/homestay/services/GlobalKpisService.java`
- [X] T015 Create `ReportController.java` skeleton in `Backend/src/main/java/com/homestay/controllers/ReportController.java`
- [X] T016 Create `AdminReportController.java` skeleton in `Backend/src/main/java/com/homestay/controllers/AdminReportController.java`
- [X] T017 Register `GET /api/v1/reports/**` for ROLE_MANAGER and `GET /api/v1/reports/global-kpis` + `/api/v1/admin/reports/**` for ROLE_ADMIN in `Backend/src/main/java/com/homestay/configs/SecurityConfig.java`
- [ ] T018 Add `app.report.timezone=Asia/Ho_Chi_Minh` default in `Backend/src/main/resources/application.yml`

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Manager KPI Dashboard theo Property (Priority: P1) 🎯 MVP

**Goal**: SCR-27 property KPI cards — rooms, occupancy, revenue, check-ins, pending approvals

**Independent Test**: Manager assigned to P → `GET /reports/property-kpis?propertyId=P` returns correct counts; other property → 403

### Implementation

- [X] T019 [US1] Implement `getPropertyKpis(managerId, propertyId)` — totalRooms, occupancyRate, current-month revenue in `PropertyKpisService.java`
- [X] T020 [US1] Add `pendingCheckIns` (today CONFIRMED) and `pendingApprovals` (payments PENDING + maintenance OPEN) in `PropertyKpisService.java`
- [X] T021 [US1] Call `ReportPropertyScopeValidator` before KPI query in `PropertyKpisService.java`
- [X] T022 [US1] Add `GET /api/v1/reports/property-kpis?propertyId=` in `ReportController.java`
- [X] T023 [P] [US1] Add `getPropertyKpis(propertyId)` to `Frontend/src/api/reportApi.ts` → `/api/v1/reports/property-kpis`
- [X] T024 [US1] Add property selector (from `propertyApi`) + wire KPI cards to live API on `Frontend/src/pages/manager/ManagerDashboardPage.tsx`
- [X] T025 [US1] Replace `managerApi.getDashboard()` monolith with `reportApi.getPropertyKpis` + existing chart data stubs or trend endpoints on `ManagerDashboardPage.tsx`
- [X] T026 [US1] Handle manager with zero assigned properties empty state on `ManagerDashboardPage.tsx`

**Checkpoint**: US1 MVP — SCR-27 KPI dashboard testable

---

## Phase 4: User Story 2 — Manager báo cáo chi tiết Property (Priority: P1)

**Goal**: SCR-44 tabs Revenue, Occupancy, Booking Trends with date range + groupBy

**Independent Test**: Manager P + 3-month range → revenue matches PAID sum; occupancy 0–100%; booking trend counts match DB

### Implementation

- [X] T027 [US2] Implement `getRevenueReport(managerId, propertyId, from, to, groupBy)` — SUM PAID, deposit/balance split in `RevenueReportService.java`
- [X] T028 [US2] Implement `byPeriod` and `byProperty` grouping (month `yyyy-MM`, week ISO) in `RevenueReportService.java`
- [X] T029 [US2] Add `GET /api/v1/reports/revenue` with query validation in `ReportController.java`
- [X] T030 [US2] Implement room-nights occupancy algorithm in `OccupancyReportService.java` per `research.md` §3
- [X] T031 [US2] Implement `byPeriod` occupancy series in `OccupancyReportService.java`
- [X] T032 [US2] Add `GET /api/v1/reports/occupancy` in `ReportController.java`
- [X] T033 [US2] Implement booking count by period for CONFIRMED/CHECKED_IN/CHECKED_OUT in `BookingTrendReportService.java`
- [X] T034 [US2] Add `GET /api/v1/reports/booking-trends` in `ReportController.java`
- [ ] T035 [US2] Add `GET /api/v1/reports/properties/{id}` summary (revenue + occupancy) in `ReportController.java`
- [X] T036 [P] [US2] Migrate `getRevenue` to `/api/v1/reports/revenue` in `Frontend/src/api/reportApi.ts`
- [X] T037 [P] [US2] Add `getOccupancy` and `getBookingTrends` methods in `Frontend/src/api/reportApi.ts`
- [X] T038 [US2] Ensure `RevenueReportPage.tsx` uses migrated API + property filter + date validation errors
- [X] T039 [US2] Replace `PROPERTIES_OCC` mock with live `getOccupancy` on `Frontend/src/pages/manager/OccupancyReportPage.tsx`
- [ ] T040 [US2] Create `BookingTrendReportPage.tsx` — filters, Recharts line/bar, property selector in `Frontend/src/pages/manager/BookingTrendReportPage.tsx`
- [X] T041 [US2] Update `/manager/reports/bookings` route to `BookingTrendReportPage` in `Frontend/src/App.tsx`
- [ ] T042 [US2] Export `BookingTrendReportPage` from `Frontend/src/pages/manager/AdminPages.tsx` (manager exports barrel)

**Checkpoint**: US2 testable — SCR-44 all three report tabs live

---

## Phase 5: User Story 3 — Admin KPI Dashboard toàn hệ thống (Priority: P1)

**Goal**: SCR-45 global KPIs — properties, floors, rooms, bookings, revenue

**Independent Test**: Admin `GET /reports/global-kpis` → counts match DB; Manager → 403

### Implementation

- [X] T043 [US3] Implement `getGlobalKpis()` — totalProperties, totalFloors, totalRooms, availableRooms, occupiedRooms in `GlobalKpisService.java`
- [X] T044 [US3] Add totalBookings, current-month totalRevenue, optional newCustomers (30d) in `GlobalKpisService.java`
- [X] T045 [US3] Add `GET /api/v1/reports/global-kpis` in `AdminReportController.java` (or `ReportController` with ADMIN guard)
- [X] T046 [P] [US3] Add `getGlobalKpis()` to `Frontend/src/api/reportApi.ts`
- [X] T047 [US3] Create `AdminDashboardPage.tsx` with KPI cards per SCR-45 in `Frontend/src/pages/admin/AdminDashboardPage.tsx`
- [X] T048 [US3] Register `/admin/dashboard` protected route ROLE_ADMIN in `Frontend/src/App.tsx`
- [X] T049 [US3] Add Admin dashboard nav link in admin layout (or `AdminPages.tsx` shell)

**Checkpoint**: US3 testable — Admin dashboard KPIs

---

## Phase 6: User Story 4 — Admin báo cáo toàn hệ thống + filter Property (Priority: P1)

**Goal**: SCR-55 yearly revenue/occupancy/booking trends with optional propertyId filter

**Independent Test**: Admin year=2026 → 12 monthly points; filter propertyId → scoped data only

### Implementation

- [X] T050 [US4] Implement `getAdminYearlyRevenue(year, propertyId)` monthly aggregation in `RevenueReportService.java`
- [X] T051 [US4] Add `GET /api/v1/admin/reports/revenue?year=&propertyId=` in `AdminReportController.java`
- [X] T052 [US4] Implement admin yearly occupancy wrapper delegating to `OccupancyReportService` in `AdminReportController.java`
- [X] T053 [US4] Add `GET /api/v1/admin/reports/occupancy?year=&propertyId=` in `AdminReportController.java`
- [X] T054 [US4] Implement admin yearly booking trends in `BookingTrendReportService.java` + endpoint `GET /api/v1/admin/reports/booking-trends` in `AdminReportController.java`
- [X] T055 [P] [US4] Add `getAdminRevenue`, `getAdminOccupancy`, `getAdminBookingTrends` to `Frontend/src/api/reportApi.ts`
- [X] T056 [US4] Create `GlobalReportsPage.tsx` — tabs Revenue/Occupancy/Booking Trends + year + property filter in `Frontend/src/pages/admin/GlobalReportsPage.tsx`
- [X] T057 [US4] Register `/admin/reports` route ROLE_ADMIN in `Frontend/src/App.tsx`
- [X] T058 [US4] Add Global Reports nav link in admin layout

**Checkpoint**: US4 testable — SCR-55 global reports with property filter

---

## Phase 7: User Story 5 — Xuất CSV doanh thu (Priority: P2)

**Goal**: Client-side CSV export matches on-screen revenue data

**Independent Test**: Export after live fetch → CSV rows match `byPeriod` table

### Implementation

- [X] T059 [US5] Verify CSV export on `RevenueReportPage.tsx` uses live `data.byPeriod` (not mock)
- [X] T060 [US5] Add empty-data guard before export on `RevenueReportPage.tsx`
- [ ] T061 [US5] Add optional CSV export button on `GlobalReportsPage.tsx` revenue tab in `Frontend/src/pages/admin/GlobalReportsPage.tsx`

**Checkpoint**: US5 testable — CSV matches displayed revenue

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Tests, quickstart validation, cleanup deprecated APIs

- [X] T062 [P] Unit test revenue aggregation matches PAID sum in `Backend/src/test/java/com/homestay/unit/RevenueReportServiceTest.java`
- [X] T063 [P] Unit test occupancy room-nights ≤1% error vs hand calc in `Backend/src/test/java/com/homestay/unit/OccupancyReportServiceTest.java`
- [ ] T064 Integration test Manager scope denial + revenue endpoint in `Backend/src/test/java/com/homestay/integration/ReportControllerIT.java`
- [ ] T065 [P] Integration test Admin global-kpis forbidden for Manager in `ReportControllerIT.java`
- [ ] T066 Run curl smoke tests in `specs/016-reporting/quickstart.md` and fix gaps
- [X] T067 [P] Mark `managerApi.getDashboard` deprecated; document migration to `reportApi.getPropertyKpis` in `Frontend/src/api/managerApi.ts`
- [X] T068 [P] Verify report routes in `Frontend/src/App.tsx` — manager + admin complete
- [X] T069 [P] Verify `ManagerLayout.tsx` Reports nav links to `/manager/reports` hub

---


---

## Closeout (2026-07-23 align-to-audit)

Gap fixes applied during /speckit-implement:

- [X] C001 Manager revenue property-scoped (getRevenueReportForManager + /api/v1/reports/revenue)
- [X] C002 Booking Trend counts only CONFIRMED/CHECKED_IN/CHECKED_OUT (FR-010)
- [X] C003 Admin propertyId on /api/admin/reports/revenue + occupancy/booking-trends yearly
- [X] C004 Remove mock OccupancyReportPage → redirect Property Reports tab
- [X] C005 Unit tests ReportServiceTest + PropertyReportServiceTest

Deferred / optional (not blocking MVP): T003 Flyway indexes, T009 dedicated date-range validator class, T035 property summary, T040 standalone BookingTrend page, T061 admin CSV, T064–T066 IT/smoke.


## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: After FR-12 PAID payments + FR-06 assignments (blockers)
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **US1 (P1)**: After Foundational — **MVP**; uses PropertyKpisService only
- **US2 (P1)**: After Foundational; shares ReportController + scope validator with US1
- **US3 (P1)**: After Foundational; independent of US1/US2 backend services except GlobalKpisService
- **US4 (P1)**: After US2 services (reuses Revenue/Occupancy/BookingTrend); Admin frontend new
- **US5 (P2)**: After US2 revenue live + US4 admin revenue tab
- **Polish (Phase 8)**: After desired user stories complete

### User Story Dependencies

```text
Foundational → US1 (KPI dashboard)
            → US2 (detailed reports) → US5 (CSV)
            → US3 (admin KPI)
            → US4 (admin reports, reuses US2 services)
```

### Parallel Opportunities

**Phase 1**: T004, T005 [P]  
**Phase 2** (after T008): T006, T007, T010–T014 all [P]  
**US1**: T023 parallel with T019–T022  
**US2**: T036, T037 parallel with backend T027–T035; T040 parallel after T037  
**US3**: T046 parallel with T043–T045  
**US4**: T055 parallel with T050–T054  
**Polish**: T062, T063, T065, T067, T068, T069 all [P]

### Parallel Example: User Story 2

```bash
T027–T029 RevenueReportService + endpoint
T030–T032 OccupancyReportService + endpoint
T033–T034 BookingTrendReportService + endpoint
T036–T037 reportApi.ts migration (parallel when contract stable)
T038–T041 frontend pages
```

### Parallel Example: User Story 4

```bash
T050–T054 AdminReportController endpoints
T055 reportApi admin methods
T056–T058 GlobalReportsPage + routes
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T005)
2. Complete Phase 2: Foundational (T006–T018)
3. Complete Phase 3: User Story 1 (T019–T026)
4. **STOP and VALIDATE**: `GET /reports/property-kpis` per `quickstart.md`
5. Demo SCR-27 Manager Dashboard

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 → Manager KPI dashboard (MVP)
3. US2 → Revenue + Occupancy + Booking Trends (SCR-44)
4. US3 → Admin global KPIs (SCR-45)
5. US4 → Admin global reports (SCR-55)
6. US5 → CSV export verify
7. Polish → Tests + quickstart

### Parallel Team Strategy

1. Team completes Setup + Foundational together
2. After Foundational:
   - Dev A: US1 + US2 Manager frontend
   - Dev B: US2 backend report services
   - Dev C: US3 + US4 Admin screens + endpoints
3. Polish when revenue validation passes

---

## Notes

- No new domain tables v1 — V032 indexes only
- Revenue timezone: Asia/Ho_Chi_Minh per `application.yml`
- `GET /api/manager/dashboard` legacy — replace with property-kpis composition
- Booking trend route was stub — fixed in T041
- Admin routes `/admin/dashboard` and `/admin/reports` likely missing — added in US3/US4
- Max date range 366 days enforced in `ReportDateRangeValidator`
- Commit after each task or logical group; stop at any checkpoint to validate story independently
