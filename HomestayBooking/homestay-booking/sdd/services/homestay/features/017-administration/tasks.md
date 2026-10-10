# Tasks: FR-17 Administration

**Input**: Design documents from `specs/017-administration/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/administration-api.yaml, quickstart.md

**Phụ thuộc**: FR-01 (JWT RBAC Customer/Admin); FR-02 (Customer display name in complaint); FR-09 (Customer Directory SCR-51 — nav link only, không implement); FR-14 (Review moderation APIs — Content Moderation tab); FR-15 (optional Outbox tab P2). **Ranh giới**: FR-17 owns Complaint + SystemSetting + ActivityLog read; FR-18 Promotion out of scope.

**Tests**: Không có phase test riêng per-story. Unit + integration trong Phase Polish per plan Phase J.

**Organization**: Tasks grouped by user story (US1–US6) for independent delivery.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no blocking deps)
- **[Story]**: US1–US6 maps to spec.md user stories

## Path Conventions

- **Backend**: `Backend/src/main/java/com/homestay/`
- **Backend tests**: `Backend/src/test/java/com/homestay/`
- **Frontend**: `Frontend/src/`
- **Migrations**: `Backend/src/main/resources/db/migration/`
- **Contract**: `specs/017-administration/contracts/administration-api.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Migration, auth blocker check, dev proxy

- [X] T001 Verify FR-01 JWT auth with **CUSTOMER** and **ADMIN** roles per `specs/001-user-auth/quickstart.md` (blocker)
- [X] T002 Create Flyway `Backend/src/main/resources/db/migration/V033__administration_fr17.sql` — tables `complaints`, `system_settings`, `activity_logs`, indexes, seed default settings per `data-model.md`
- [X] T003 [P] Confirm Vite proxy `/api/v1` → backend in `Frontend/vite.config.ts`
- [X] T004 [P] Add dev seed SQL comments in V033 — sample Customer + Admin users referenced for complaint smoke tests

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Entities, repositories, shared ActivityLog writer, DTOs, security — MUST complete before user story phases

**⚠️ CRITICAL**: No user story work until this phase complete

- [X] T005 [P] Create `ComplaintStatus.java` enum in `Backend/src/main/java/com/homestay/enums/ComplaintStatus.java` — OPEN, INVESTIGATING, RESOLVED, CLOSED
- [X] T006 [P] Create `Complaint.java` entity in `Backend/src/main/java/com/homestay/entity/Complaint.java` per `data-model.md`
- [X] T007 [P] Create `SystemSetting.java` entity in `Backend/src/main/java/com/homestay/entity/SystemSetting.java`
- [X] T008 [P] Create `ActivityLog.java` entity in `Backend/src/main/java/com/homestay/entity/ActivityLog.java` (+ ipAddress field)
- [X] T009 [P] Create `ComplaintRepository.java` in `Backend/src/main/java/com/homestay/repository/ComplaintRepository.java` — findByUserId, findByStatus, search specs
- [X] T010 [P] Create `SystemSettingRepository.java` in `Backend/src/main/java/com/homestay/repository/SystemSettingRepository.java` — findByKey
- [X] T011 [P] Create `ActivityLogRepository.java` in `Backend/src/main/java/com/homestay/repository/ActivityLogRepository.java` — date/action/user filters
- [X] T012 Create `ActivityLogService.java` in `Backend/src/main/java/com/homestay/service/ActivityLogService.java` — shared `log(userId, action, entityType, entityId, details, ip)` append-only writer
- [X] T013 Create `ComplaintStatusValidator.java` in `Backend/src/main/java/com/homestay/service/ComplaintStatusValidator.java` — linear transition rules + RESOLVED requires notes (fixed single-arg BusinessException)
- [X] T014 [P] Create administration DTOs — `CreateComplaintRequest`, `ComplaintSummaryResponse`, `ComplaintDetailResponse`, `UpdateComplaintStatusRequest`, `SystemSettingsResponse`, `UpdateSystemSettingsRequest`, `ActivityLogEntryResponse`
- [X] T015 Register endpoints via @PreAuthorize ADMIN/CUSTOMER on controllers (dual-mapped /api/admin + /api/v1/admin)
- [X] T016 [P] Create `ComplaintService.java` in `Backend/src/main/java/com/homestay/service/ComplaintService.java`
- [X] T017 [P] `AdminSettingsService.java` expanded with full settings + getDepositPercentage()
- [X] T018 [P] Create `ActivityLogQueryService.java` in `Backend/src/main/java/com/homestay/service/ActivityLogQueryService.java`

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Customer gửi và theo dõi khiếu nại (Priority: P1) 🎯 MVP

**Goal**: Customer create complaint + view own list/detail with OPEN status

**Independent Test**: Customer POST complaint → appears in My Complaints OPEN; cannot view another customer's complaint

### Implementation

- [X] T019 [US1] Implement `createComplaint(customerId, subject, description)` — validate lengths, status OPEN in `ComplaintService.java`
- [X] T020 [US1] Implement `listMyComplaints(customerId, pageable)` and `getMyComplaintDetail(customerId, id)` with owner scope in `ComplaintService.java`
- [X] T021 [US1] Log `COMPLAINT_CREATED` via `ActivityLogService` in `ComplaintService.createComplaint()`
- [X] T022 [US1] Create `ComplaintController.java` with `POST /api/v1/complaints` and `GET /api/v1/complaints`, `GET /api/v1/complaints/{id}`
- [X] T023 [P] [US1] Migrate `submitComplaint`, `getMyComplaints`, `getComplaintDetail` to `/api/v1/complaints` in `Frontend/src/api/complaintsApi.ts`
- [X] T024 [US1] Wire live API on `Frontend/src/pages/customer/CustomerComplaintPages.tsx` — list, create form, validation errors (min 5/20 chars)
- [X] T025 [US1] Verify customer complaint routes `/customer/complaints` and `/customer/complaints/create` in `Frontend/src/App.tsx`

**Checkpoint**: US1 MVP — Customer complaint flow testable via `quickstart.md` smoke test

---

## Phase 4: User Story 2 — Admin quản lý và giải quyết khiếu nại (Priority: P1)

**Goal**: SCR-54 Admin complaint list, detail, status workflow OPEN → INVESTIGATING → RESOLVED → CLOSED

**Independent Test**: Admin full workflow; Customer sees updated status; Manager/Customer get 403 on admin APIs

### Implementation

- [X] T026 [US2] Implement `listAllComplaints(status, search, pageable)` with customer name join in `AdminComplaintService.java`
- [X] T027 [US2] Implement `getAdminComplaintDetail(id)` with customer `{ id, fullName, email }` in `AdminComplaintService.java`
- [X] T028 [US2] Implement `updateComplaintStatus(adminId, id, status, resolutionNotes)` — validator, set `resolvedAt` on RESOLVED in `AdminComplaintService.java`
- [X] T029 [US2] Log `COMPLAINT_STATUS_CHANGED` via `ActivityLogService` in `AdminComplaintService.updateComplaintStatus()`
- [X] T030 [US2] Create `AdminComplaintController.java` with `GET /api/v1/admin/complaints`, `GET /api/v1/admin/complaints/{id}`, `PUT/PATCH /api/v1/admin/complaints/{id}/status`
- [X] T031 [US2] Add deprecated legacy `PATCH /api/v1/admin/complaints/{id}/resolve` mapping `{ resolution }` → RESOLVED
- [X] T032 [P] [US2] Add `getAdminComplaints`, `getAdminComplaintDetail`, `updateComplaintStatus` to `Frontend/src/api/complaintsApi.ts`
- [X] T033 [US2] Enhanced `AdminComplaintsPage.tsx` with full status workflow (Investigate → Resolve with notes ≥10 → Close)
- [X] T034 [US2] Drawer detail + status actions + resolutionNotes form in `AdminComplaintsPage.tsx`
- [X] T035 [US2] AdminLayout + live admin API wired on `AdminComplaintsPage.tsx`
- [X] T036 [US2] `/admin/complaints` route kept; Manager complaint PATCH returns 410 GONE; ManagerLayout nav + App.tsx `/manager/complaints` routes removed
- [X] T037 [US2] Manager complaint page imports removed from App.tsx (pages left as orphaned legacy files)

**Checkpoint**: US2 testable — SCR-54 Admin complaint management end-to-end

---

## Phase 5: User Story 3 — Admin cấu hình System Settings (Priority: P1)

**Goal**: SCR-56 System Settings tab — deposit %, system name, support email, bank info

**Independent Test**: Admin PUT settings → reload shows persisted values; invalid deposit % rejected; SETTINGS_UPDATED in activity log

### Implementation

- [X] T038 [US3] Implement `getSettings()` — aggregate keys into `SystemSettingsResponse` in `AdminSettingsService.java`
- [X] T039 [US3] Implement `updateSettings(adminId, request)` — partial upsert, deposit 10–50%, email validation in `AdminSettingsService.java`
- [X] T040 [US3] Add public `getDepositPercentage()` accessor for FR-04/12 consumers in `AdminSettingsService.java`; wired to BookingService
- [X] T041 [US3] Log `SETTINGS_UPDATED` via `ActivityLogService` in `AdminSettingsService.updateSettings()`
- [X] T042 [US3] Expanded `AdminSettingsController.java` dual-mapped `GET/PUT /api/v1/admin/settings` + `/api/admin/settings`
- [X] T043 [P] [US3] Create `settingsApi.ts` with `getSettings` and `updateSettings` in `Frontend/src/api/settingsApi.ts`
- [X] T044 [US3] `SystemSettingsTab` in SystemAdminPage — deposit %, system name, support email, bank fields, updatedAt
- [X] T045 [US3] Wire save/load + validation feedback on `SystemSettingsTab` via `settingsApi.ts`

**Checkpoint**: US3 testable — System Settings GET/PUT per `quickstart.md`

---

## Phase 6: User Story 4 — Admin xem Activity Logs (Priority: P1)

**Goal**: SCR-56 Activity Logs tab — paginated read-only audit with date/action filters

**Independent Test**: Admin opens logs → newest first; date filter narrows results; no edit/delete actions

### Implementation

- [X] T046 [US4] Implement `listActivityLogs(from, to, action, userId, search, pageable)` with user join in `ActivityLogQueryService.java`
- [X] T047 [US4] Create `AdminActivityLogController.java` with `GET /api/v1/admin/activity-logs` dual-mapped
- [X] T048 [P] [US4] Create `activityLogApi.ts` with `getLogs` in `Frontend/src/api/activityLogApi.ts`
- [X] T049 [US4] `ActivityLogsTab` in SystemAdminPage — paginated table, date range, search filter, live API
- [X] T050 [US4] Empty state + read-only (no delete/edit UI) on `ActivityLogsTab`

**Checkpoint**: US4 testable — Activity Logs tab with live API

---

## Phase 7: User Story 5 — Admin kiểm duyệt đánh giá toàn hệ thống (Priority: P1)

**Goal**: SCR-56 Content Moderation tab — wire FR-14 admin review Hide/Show (no duplicate backend)

**Independent Test**: Admin tab lists all reviews; Hide Published → Hidden; Show restores Published (requires FR-14 backend)

### Implementation

- [X] T051 [US5] `ContentModerationPanel` in SystemAdminPage wired to `reviewApi.getAdminReviews` / `moderateAdminReview`
- [X] T052 [US5] All/Published/Hidden filter + Hide/Show confirmation dialog in `ContentModerationPanel`
- [X] T053 [US5] Error state handled when admin reviews API unavailable

**Checkpoint**: US5 testable — Content Moderation tab (blocked until FR-14 backend live)

---

## Phase 8: User Story 6 — Admin hub System Administration (Priority: P2)

**Goal**: SCR-56 unified `/admin/system` with tabs; optional Outbox link (FR-15 P2)

**Independent Test**: Admin switches tabs without full reload; non-Admin denied; each tab loads correct content

### Implementation

- [X] T054 [US6] `SystemAdminPage.tsx` refactored with tabs: Settings, Activity Logs, Content Moderation, Outbox Failures
- [X] T055 [US6] All tab panels mounted inline in `SystemAdminPage.tsx`
- [X] T056 [US6] AdminLayout nav updated: "System Administration" → `/admin/system`
- [X] T057 [US6] Register `/admin/system` route with Admin role guard; `/admin/settings` redirects to `/admin/system`
- [X] T058 [US6] Admin nav: "System Administration" + "Complaints" links present
- [X] T059 [P] [US6] Outbox Monitor tab integrated (uses outboxAdminApi)
- [X] T060 [P] [US6] Customers nav already present in AdminLayout

**Checkpoint**: US6 testable — SCR-56 hub navigation complete

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Tests, quickstart validation, route cleanup, deprecated API removal

- [X] T061 [P] Unit test complaint status transitions + RESOLVED notes required in `ComplaintStatusValidatorTest.java`
- [X] T062 [P] Unit test deposit percentage 10–50% and email validation in `AdminSettingsServiceTest.java`
- [ ] T063 Integration test customer create + admin workflow OPEN→CLOSED (skipped — no testcontainers setup)
- [ ] T064 [P] Integration test Customer/Manager 403 on `/admin/settings` and `/admin/complaints` (skipped — no testcontainers)
- [ ] T065 [P] Integration test Customer cannot GET another customer's complaint (skipped — no testcontainers)
- [ ] T066 Run curl smoke tests in `specs/017-administration/quickstart.md` and fix gaps
- [X] T067 [P] Activity Logs tab now uses live API; mock data removed from SystemAdminPage
- [X] T068 [P] Verify all admin routes in `Frontend/src/App.tsx` — `/admin/complaints`, `/admin/system`
- [X] T069 [P] Remove deprecated `/manager/complaints` nav entry from `Frontend/src/layouts/ManagerLayout.tsx`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: After FR-01 auth (blocker)
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **US1 (P1)**: After Foundational — **MVP**; creates complaints + ActivityLog entries
- **US2 (P1)**: After US1 (needs complaints to resolve); shares `ComplaintService`
- **US3 (P1)**: After Foundational; independent of US1/US2 (uses ActivityLogService)
- **US4 (P1)**: After Foundational; benefits from US1/US3 log entries for demo data
- **US5 (P1)**: After Foundational frontend; **blocked by FR-14** backend for live data
- **US6 (P2)**: After US3–US5 tab components exist
- **Polish (Phase 9)**: After desired user stories complete

### User Story Dependencies

```text
Foundational → US1 (Customer complaints) → US2 (Admin complaints)
            → US3 (System Settings)
            → US4 (Activity Logs)
            → US5 (Content Moderation tab — FR-14 APIs)
            → US6 (SCR-56 hub — integrates US3–US5 tabs)
```

### Parallel Opportunities

**Phase 1**: T003, T004 [P]  
**Phase 2** (after T012): T005–T011, T014, T016–T018 all [P]  
**US1**: T023 parallel with T019–T022  
**US2**: T032 parallel with T026–T031; T033–T034 parallel after T032  
**US3**: T043 parallel with T038–T042  
**US4**: T048 parallel with T046–T047  
**US5**: T051–T053 sequential (same file)  
**US6**: T059, T060 [P]  
**Polish**: T061, T062, T064, T065, T067, T068, T069 all [P]

### Parallel Example: User Story 2

```bash
T026–T031 ComplaintService admin methods + AdminComplaintController
T032 complaintsApi.ts admin methods (parallel when DTO stable)
T033–T035 Move pages to admin/ + AdminLayout + live API
T036–T037 App.tsx routes + barrel cleanup
```

### Parallel Example: Foundational

```bash
T005–T011 Entities + repositories (all [P])
T014 DTOs [P]
T016–T018 Service skeletons [P]
T012 ActivityLogService → T013 ComplaintStatusValidator → T015 SecurityConfig
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T004)
2. Complete Phase 2: Foundational (T005–T018)
3. Complete Phase 3: User Story 1 (T019–T025)
4. **STOP and VALIDATE**: Customer POST/GET complaints per `quickstart.md`
5. Demo Customer complaint create + My Complaints list

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 → Customer complaints (MVP)
3. US2 → Admin SCR-54 complaint resolution
4. US3 → System Settings tab
5. US4 → Activity Logs tab
6. US5 → Content Moderation tab (FR-14)
7. US6 → SCR-56 unified hub + admin nav
8. Polish → Tests + quickstart + route cleanup

### Parallel Team Strategy

With multiple developers after Foundational:

- **Developer A**: US1 → US2 (complaint vertical slice)
- **Developer B**: US3 + US4 (settings + logs)
- **Developer C**: US5 + US6 (moderation tab + hub assembly)

---

## Notes

- FR-09 Customer Directory: link only in `AdminLayout` — implement in `specs/009-customer-management`
- FR-14 admin review APIs: US5 frontend only; backend tasks in `specs/014-review-rating/tasks.md`
- FR-15 Outbox tab: optional P2 in US6 — skip if notification engine not ready
- Deposit % consumer wiring in FR-04/12 is out of FR-17 scope but `getDepositPercentage()` must exist
- Manager `/manager/complaints` routes are **incorrect actor** — remove after US2 migration
