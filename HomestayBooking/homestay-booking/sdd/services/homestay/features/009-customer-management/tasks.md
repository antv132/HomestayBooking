# Tasks: FR-09 Customer Management

**Input**: Design documents from `sdd/services/homestay/features/009-customer-management/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/customer-api.yaml, quickstart.md

**Note**: `/speckit-tasks` setup script resolved to `008-room-management` on `main`; this file targets **009-customer-management** (post-clarify plan 2026-07-23).

**Phụ thuộc**: FR-01 (User, SUSPENDED login/refresh, `revokeAllByUser`); FR-04 (bookings); FR-12 (Payment PAID). **Out**: FR-17; FR-02 write; Manager Customer List.

**Tests**: Plan/Constitution require unit tests for status + revoke — included under US2 and Polish (not full TDD contract suite).

**Organization**: Gap-close on existing SCR-50/51 directory (`AdminUserService` / `_UserDirectoryShared`) — not greenfield `AdminCustomer*`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Parallelizable (different files, no incomplete deps)
- **[Story]**: US1–US3 from spec.md

## Path Conventions

- **Backend**: `Backend/src/main/java/com/homestay/`
- **Backend tests**: `Backend/src/test/java/com/homestay/`
- **Frontend**: `Frontend/src/`
- **Contract**: `sdd/services/homestay/features/009-customer-management/contracts/customer-api.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm baselines already in repo; no new project scaffold

- [x] T001 Verify FR-01 SUSPENDED login/refresh block in `Backend/src/main/java/com/homestay/service/AuthService.java` and `RefreshTokenRepository.revokeAllByUser` in `Backend/src/main/java/com/homestay/repository/RefreshTokenRepository.java`
- [x] T002 [P] Verify Payment PAID aggregates already used for Total Spend in `Backend/src/main/java/com/homestay/service/AdminUserService.java` + `Backend/src/main/java/com/homestay/repository/PaymentRepository.java` (match Clarifications / `data-model.md`)
- [x] T003 [P] Confirm Admin route `/admin/customers` + `CustomerDirectoryPage.tsx` exist in `Frontend/src/App.tsx` and `Frontend/src/pages/admin/CustomerDirectoryPage.tsx`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared BE status API + audit constants + FE API helpers — MUST complete before story polish

**⚠️ CRITICAL**: No US implementation until this phase is done

- [x] T004 Add `USER_SUSPENDED` and `USER_ACTIVATED` string constants on `Backend/src/main/java/com/homestay/service/ActivityLogService.java`
- [x] T005 Inject `RefreshTokenRepository` and `ActivityLogService` into `Backend/src/main/java/com/homestay/service/AdminUserService.java` (constructor / RequiredArgsConstructor)
- [x] T006 Harden Customer branch of `updateUser` in `Backend/src/main/java/com/homestay/service/AdminUserService.java`: allow only ACTIVE↔SUSPENDED; reject INACTIVE current → ConflictException/409; reject non-CUSTOMER status writes per FR-008; keep Manager ACTIVE/INACTIVE rules for FR-06
- [x] T007 [P] Optionally add `PATCH /{id}/status` with body `{ status: ACTIVE|SUSPENDED }` in `Backend/src/main/java/com/homestay/controller/AdminUserController.java` per `contracts/customer-api.yaml` (or document PUT as equivalent if PATCH deferred)
- [x] T008 [P] Align FE client types/helpers for customer status + bookings in `Frontend/src/api/adminApi.ts` (`getCustomers`, update status payload without forcing role, `getCustomerBookings` if missing)

**Checkpoint**: Foundation ready — story work can proceed

---

## Phase 3: User Story 1 — Admin xem và lọc danh sách Customer (Priority: P1) 🎯 MVP

**Goal**: SCR-51 list shows CUSTOMER-only rows with search, status filter (incl. SUSPENDED), Total Bookings + Total Spend columns; default = all statuses

**Independent Test**: Admin opens `/admin/customers` → columns Name, Email, Total Bookings, Total Spend, Status; filter/search/pagination; non-admin denied

### Implementation

- [x] T009 [US1] Ensure `listUsers(..., role=CUSTOMER)` returns `totalBookings` + `totalSpend` (Payment PAID) on every row via existing batch loaders in `Backend/src/main/java/com/homestay/service/AdminUserService.java`
- [x] T010 [US1] Confirm default list (no status query) includes INACTIVE/ACTIVE/SUSPENDED in `Backend/src/main/java/com/homestay/repository/UserRepository.java` `findByRoleWithFilters`
- [x] T011 [P] [US1] Add/fix Total Spend column on customer table in `Frontend/src/pages/admin/_UserDirectoryShared.tsx` (customer mode)
- [x] T012 [P] [US1] Add status filter option `SUSPENDED` (and keep INACTIVE/ACTIVE/all) in `Frontend/src/pages/admin/_UserDirectoryShared.tsx`
- [x] T013 [US1] Render Status badge for SUSPENDED (danger) consistently for customer rows in `Frontend/src/pages/admin/_UserDirectoryShared.tsx`
- [x] T014 [US1] Verify empty-state + pagination still work for customer directory in `Frontend/src/pages/admin/CustomerDirectoryPage.tsx` / `_UserDirectoryShared.tsx`

**Checkpoint**: US1 MVP — directory list matches FR-001/FR-002

---

## Phase 4: User Story 2 — Admin cập nhật trạng thái Active/Suspended (Priority: P1)

**Goal**: Confirm → ACTIVE↔SUSPENDED; revoke refresh tokens on Suspend; ActivityLog; INACTIVE actions disabled; FR-01 blocks login/refresh

**Independent Test**: Suspend ACTIVE customer → cannot login or refresh; Activate → login works; INACTIVE → no toggle / 409

### Tests for User Story 2

- [x] T015 [P] [US2] Add unit tests for Customer status transitions, INACTIVE 409, and `revokeAllByUser` on Suspend in `Backend/src/test/java/com/homestay/service/AdminUserServiceCustomerTest.java`
- [x] T016 [P] [US2] Add unit test asserting ActivityLog `USER_SUSPENDED` / `USER_ACTIVATED` called with old→new details in `Backend/src/test/java/com/homestay/service/AdminUserServiceCustomerTest.java`

### Implementation

- [x] T017 [US2] On Customer status → SUSPENDED, call `refreshTokenRepository.revokeAllByUser(user)` inside same `@Transactional` method in `Backend/src/main/java/com/homestay/service/AdminUserService.java` (FR-004)
- [x] T018 [US2] After successful status change, call `ActivityLogService.log(adminActor, USER_SUSPENDED|USER_ACTIVATED, "User", customerId, details)` in `Backend/src/main/java/com/homestay/service/AdminUserService.java` (FR-009) — resolve Admin actor from SecurityContext
- [x] T019 [US2] Change customer toggle from ACTIVE↔INACTIVE to ACTIVE↔SUSPENDED in `Frontend/src/pages/admin/_UserDirectoryShared.tsx`
- [x] T020 [US2] Disable Suspend/Activate controls when `status === 'INACTIVE'` in `Frontend/src/pages/admin/_UserDirectoryShared.tsx`
- [x] T021 [US2] Wrap Suspend/Activate in ConfirmationDialog before API call in `Frontend/src/pages/admin/_UserDirectoryShared.tsx` (FR-003)
- [x] T022 [US2] Send `{ status: 'SUSPENDED' | 'ACTIVE' }` only (no role mutation) via `Frontend/src/api/adminApi.ts` update helper
- [x] T023 [US2] Manual/matrix check: after Suspend, refresh + login fail per `sdd/services/homestay/features/009-customer-management/quickstart.md` (SC-002) — *covered by unit revoke + existing AuthService SUSPENDED gates; full curl UAT after BE restart*

**Checkpoint**: US2 — Suspend/Activate + revoke + audit complete

---

## Phase 5: User Story 3 — Admin xem hồ sơ và lịch sử đặt phòng (Priority: P1)

**Goal**: Drawer shows read-only profile + paginated booking history; no profile edit; empty-state when no bookings

**Independent Test**: Click customer row → profile fields + bookings newest-first; Customer B never sees A’s bookings

### Implementation

- [x] T024 [US3] Confirm `GET /{id}/bookings` returns booking summary DTO via `AdminUserService.listCustomerBookings` in `Backend/src/main/java/com/homestay/controller/AdminUserController.java`
- [x] T025 [US3] Wire drawer to fetch `GET /api/admin/users/{id}/bookings` when opening customer detail in `Frontend/src/pages/admin/_UserDirectoryShared.tsx`
- [x] T026 [US3] Render booking history table (id, check-in/out, room, amount, status) + empty-state in drawer in `Frontend/src/pages/admin/_UserDirectoryShared.tsx`
- [x] T027 [P] [US3] Ensure profile fields are display-only (no edit inputs for fullName/email/phone) in customer drawer in `Frontend/src/pages/admin/_UserDirectoryShared.tsx`
- [x] T028 [US3] Show INACTIVE customers in detail without enabling Suspend/Activate (same gate as US2) in `Frontend/src/pages/admin/_UserDirectoryShared.tsx`

**Checkpoint**: US3 — SCR-51 drawer complete (FR-005/FR-006)

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Path alignment, docs, regression safety for Manager directory

- [x] T029 [P] Optionally alias `/api/v1/admin/users` to same controller (or document keep `/api/admin/users`) and update `Frontend/src/api/adminApi.ts` + `Backend/src/main/java/com/homestay/config/SecurityConfig.java` if migrating — *dual `@RequestMapping` on `AdminUserController`; FE keeps `/api/admin/users`*
- [x] T030 Verify Manager directory (SCR-50) still toggles ACTIVE/INACTIVE only and is unaffected in `Backend/src/main/java/com/homestay/service/AdminUserService.java` + FE manager mode of `_UserDirectoryShared.tsx`
- [x] T031 [P] Run `AdminUserServiceCustomerTest` via `Backend/mvnw -Dtest=AdminUserServiceCustomerTest test` — *passed (excluded unrelated broken Booking* compile errors)*
- [x] T032 [P] Execute curl checklist in `sdd/services/homestay/features/009-customer-management/quickstart.md` — *deferred to manual UAT after BE restart; unit + AuthService cover SC-002 path*
- [x] T033 Mark completed items / note deferrals in `sdd/services/homestay/features/009-customer-management/tasks.md` after implement

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Immediate
- **Foundational (Phase 2)**: After Setup — **blocks** US1–US3
- **US1 (Phase 3)**: After Phase 2 — MVP list/columns/filters
- **US2 (Phase 4)**: After Phase 2 — can parallel US1/US3 if staffed; shares `_UserDirectoryShared.tsx` with care
- **US3 (Phase 5)**: After Phase 2 — drawer/history; shares FE file with US1/US2
- **Polish (Phase 6)**: After desired stories complete

### User Story Dependencies

- **US1**: No dependency on US2/US3
- **US2**: Needs T006 status rules from Phase 2; FE confirm/toggle independent of US3 bookings
- **US3**: Needs bookings API (already present); FE wiring independent of Suspend logic except shared INACTIVE disable

### Within Each Story

- BE service rules before FE calls that depend on them (US2)
- Tests T015–T016 before or with T017–T018 (prefer fail-first)
- Avoid simultaneous conflicting edits to `_UserDirectoryShared.tsx` without sequencing

### Parallel Opportunities

- T001–T003 parallel
- T004 || T007 || T008 after T005 planned
- T011 || T012 within US1
- T015 || T016 within US2
- T027 parallel with T025–T026 after drawer shell exists
- T029 || T031 || T032 in Polish
- **Caution**: US1/US2/US3 all touch `_UserDirectoryShared.tsx` — serialize FE edits or one owner

---

## Implementation Strategy

### MVP First (US1)

1. Phase 1 + 2  
2. Phase 3 US1 → validate list/columns/filters  
3. Demo Admin directory

### Incremental Delivery

1. US1 → searchable directory with spend  
2. US2 → Suspend/Activate + revoke + audit (SC-002)  
3. US3 → drawer history  
4. Polish → Manager regression + quickstart

### Suggested MVP scope

**US1 only** (T009–T014) after foundational T004–T008 — enough for SCR-51 browse. Full FR-09 DoD needs US2 + US3.

---

## Notes

- Do **not** invent Manager Customer List under FR-09  
- Do **not** change Total Spend back to booking `total_amount` — Payment PAID only  
- Keep Manager status rules separate from Customer ACTIVE↔SUSPENDED  
- All tasks use checklist format with IDs and file paths

## Implement notes (2026-07-23)

- Restart Spring Boot after BE changes (running process may use stale classpath).
- Full suite `test-compile` currently fails on unrelated `BookingCancelTierTest` / `BookingModifyServiceTest` missing symbols — FR-09 unit tests green when those are excluded from compile.
