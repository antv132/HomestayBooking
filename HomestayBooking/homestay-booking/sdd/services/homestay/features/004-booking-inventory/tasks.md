# Tasks: FR-04 Booking & Inventory Management

**Input**: Design documents from `sdd/services/homestay/features/004-booking-inventory/`  
**Prerequisites**: plan.md, spec.md (Clarifications 2026-07-23), research.md, data-model.md, contracts/, quickstart.md

**Context**: Baseline booking/payment/jobs/FE largely **already implemented**. This task list is the **align-to-clarify closeout** (half-open inventory + RoomStatusBlock gate) plus per-story verification. Do **not** re-scaffold entities/controllers that already exist under `Backend/src/main/java/com/homestay/`.

**Tests**: Included where plan § Implementation Notes / Constitution V require coverage for overlap + blocks (not full TDD).

**Organization**: By user story (US1–US7). US5 = deferred edu (TD-011) — documentation only.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Parallelizable (different files, no incomplete deps)
- **[Story]**: US1–US7 from spec.md

## Path Conventions

- **Backend**: `Backend/src/main/java/com/homestay/`
- **Backend tests**: `Backend/src/test/java/com/homestay/`
- **Frontend**: `Frontend/src/`
- **Feature docs**: `sdd/services/homestay/features/004-booking-inventory/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm env/docs match clarify; no greenfield scaffold

- [X] T001 [P] Confirm `app.booking.hold-timeout-minutes` default 10 and `no-show-after-hours` default 24 in `Backend/src/main/resources/application.yml`
- [X] T002 [P] Confirm clarify smoke steps in `sdd/services/homestay/features/004-booking-inventory/quickstart.md` (half-open turnover + RoomStatusBlock 409)
- [X] T003 [P] Confirm agent plan pointer in `.cursor/rules/specify-rules.mdc` targets `004-booking-inventory/plan.md`

---

## Phase 2: Foundational (Align-to-clarify — BLOCKS stories)

**Purpose**: Inventory write-path must match Spec Sync 2026-07-23 before story sign-off

**⚠️ CRITICAL**: Complete before marking any US Done against clarify

- [X] T004 Fix half-open overlap in `Backend/src/main/java/com/homestay/repository/RoomRepository.java` — `existsOverlapBooking` and `existsOverlapBookingExcluding` use `checkIn < otherCheckOut AND checkOut > otherCheckIn` (exclude CANCELLED/CHECKED_OUT/NO_SHOW)
- [X] T005 [P] Align room-search / availability EXISTS overlap in `Backend/src/main/java/com/homestay/repository/RoomRepository.java` (and any duplicate inclusive predicates) to the same half-open rule
- [X] T006 Wire `RoomStatusBlockRepository.existsActiveOverlappingStay` into `BookingService.createBooking` in `Backend/src/main/java/com/homestay/service/BookingService.java` after room lock; on true throw ConflictException (409)
- [X] T007 Wire `existsActiveOverlappingStay` into `BookingService.modifyBookingForManager` in `Backend/src/main/java/com/homestay/service/BookingService.java` for the target room + new dates (exclude self from booking overlap only)
- [X] T008 Inject `RoomStatusBlockRepository` via constructor in `Backend/src/main/java/com/homestay/service/BookingService.java` (no field injection)
- [X] T009 [P] Add unit tests for half-open same-day turnover + block conflict in `Backend/src/test/java/com/homestay/service/BookingInventoryAlignTest.java` (or extend `BookingServiceCoverageTest.java` / `BookingModifyServiceTest.java`)
- [X] T010 Update `sdd/services/homestay/features/004-booking-inventory/data-model.md` create-step comments if JPQL snippets still show inclusive bounds

**Checkpoint**: Create A=`[D,D+2)`, B=`[D+2,D+4)` both succeed; create overlapping RoomStatusBlock stay → 409

---

## Phase 3: User Story 1 — Customer đặt phòng + cọc VNPay (Priority: P1) 🎯 MVP

**Goal**: Create PENDING_DEPOSIT, 40/60 snapshot, half-open lock, VNPay Return URL → CONFIRMED + contract outbox; reject block/overlap

**Independent Test**: Create booking → PENDING_DEPOSIT + holdExpiresAt; concurrent overlap → one 409; VNPay return → CONFIRMED; block overlap → 409; same-day turnover OK

### Implementation / alignment

- [X] T011 [US1] Verify `BookingService.createBooking` snapshot 40/60 + `holdExpiresAt` in `Backend/src/main/java/com/homestay/service/BookingService.java` matches FR-003
- [X] T012 [US1] Verify deposit Confirm path in `Backend/src/main/java/com/homestay/service/PaymentService.java` (Return URL only; no IPN) sets CONFIRMED + outbox contract enqueue
- [X] T013 [P] [US1] Verify customer create UX in `Frontend/src/pages/customer/BookingFormPage.tsx` + deposit flow `Frontend/src/pages/customer/DepositPaymentPage.tsx` / `VNPayResultPage.tsx`
- [X] T014 [US1] Smoke per `quickstart.md` create + VNPay sandbox return against running `HomestayApplication`

**Checkpoint**: US1 clarify-compliant independently

---

## Phase 4: User Story 2 — Customer xem booking (Priority: P1)

**Goal**: Own list/detail with amounts + status actions (VNPay deposit CTA when PENDING_DEPOSIT)

**Independent Test**: `GET` my bookings only own; detail shows total/paid/remaining/damageFeeAmount; other customer id → 403

### Implementation / alignment

- [X] T015 [P] [US2] Verify ownership checks in `Backend/src/main/java/com/homestay/controller/BookingV1Controller.java` + `BookingService.getBookingDetail`
- [X] T016 [P] [US2] Ensure detail breakdown shows `remainingAmount` and `damageFeeAmount` separately in `Frontend/src/pages/customer/BookingPages.tsx` (and DTO `BookingDetailResponse.java` if fields missing)
- [X] T017 [US2] Verify list filters/status badges in `Frontend/src/pages/customer/BookingListPage.tsx`

**Checkpoint**: US2 readable without support (SC-007)

---

## Phase 5: User Story 3 — Manager check-in / check-out (Priority: P1)

**Goal**: Scoped list; check-in with remaining/cash desk; checkout gated by inspection + unpaid DAMAGE_FEE; HK task on checkout

**Independent Test**: Manager property scope; check-in needs remaining paid or `remainingCollected`; checkout blocked until inspection/damage PAID; then PENDING_CLEANING + HousekeepingTask

### Implementation / alignment

- [X] T018 [US3] Verify `markAsCheckedInForManager` cash path in `Backend/src/main/java/com/homestay/service/BookingService.java` (`remainingCollected` → CASH payment)
- [X] T019 [US3] Verify `assertCheckoutInspectionGate` keeps damage as separate `DAMAGE_FEE` (does not mutate `remainingAmount`) in `BookingService.java`
- [X] T020 [P] [US3] Verify FE check-in/out in `Frontend/src/pages/manager/BookingCheckInOutPage.tsx` uses `/api/v1/manager/bookings/...`
- [X] T021 [US3] Confirm legacy `BookingController` check-in/out remain blocked in `Backend/src/main/java/com/homestay/service/BookingService.java`

**Checkpoint**: US3 operational path works under clarify FR-013

---

## Phase 6: User Story 4 — Customer hủy theo tier (Priority: P2)

**Goal**: Preview + cancel: ≥7 → 100%; `3≤d<7` → 50%; `<3` → 0%; day 7 = 100%

**Independent Test**: Preview at 7 days shows 100%; at 5 days 50%; Checked-in cancel rejected

### Implementation / alignment

- [X] T022 [US4] Confirm `resolveCustomerCancelRefundPercent` in `Backend/src/main/java/com/homestay/service/BookingService.java` matches clarify (`>=7` → 100)
- [X] T023 [P] [US4] Align cancel policy copy in `BookingService.customerCancelPolicyText` + `Frontend/src/pages/customer/BookingPages.tsx` to `3≤d<7` wording (not “3–7” inclusive of day 7)
- [X] T024 [US4] Confirm `BookingCancelTierTest.java` covers day-7 = 100% in `Backend/src/test/java/com/homestay/service/BookingCancelTierTest.java`

**Checkpoint**: SC-005 tier display accurate

---

## Phase 7: User Story 5 — CK + biên lai (Priority: P2) — DEFERRED

**Goal**: Out of edu scope (TD-011)

**Independent Test**: N/A

- [X] T025 [US5] Document skip: ensure no new SCR-20 receipt / Manager CK verify UX; leave `PaymentService.verifyPayment` stub messaging referencing TD-011 in `Backend/src/main/java/com/homestay/service/PaymentService.java` (no feature build)

**Checkpoint**: US5 remains deferred

---

## Phase 8: User Story 6 — Manager sửa Confirmed (Priority: P2)

**Goal**: Modify dates/room with half-open + RoomStatusBlock; price delta; block non-CONFIRMED

**Independent Test**: Modify to free dates OK; modify into block or overlap → 409; Checked-in modify rejected

### Implementation / alignment

- [X] T026 [US6] After T007, verify modify conflict messages in `Backend/src/main/java/com/homestay/service/BookingService.java` / `ModifyBookingResponse.java`
- [X] T027 [P] [US6] Verify manager modify UI in `Frontend/src/pages/manager/BookingMgmtDetailPage.tsx` calls v1 modify endpoint
- [X] T028 [US6] Extend `BookingModifyServiceTest.java` for RoomStatusBlock conflict in `Backend/src/test/java/com/homestay/service/BookingModifyServiceTest.java`

**Checkpoint**: US6 clarify-compliant

---

## Phase 9: User Story 7 — Hold timeout + No-show (Priority: P3)

**Goal**: Auto-cancel unpaid holds; NO_SHOW after `checkInDate.atStartOfDay()+24h`

**Independent Test**: Expired unpaid PENDING_DEPOSIT → CANCELLED; CONFIRMED past deadline → NO_SHOW; deposit unpaid skipped if PAID race

### Implementation / alignment

- [X] T029 [P] [US7] Verify `BookingHoldTimeoutJob` + `cancelExpiredDepositHolds` in `Backend/src/main/java/com/homestay/job/BookingHoldTimeoutJob.java` / `BookingService.java`
- [X] T030 [P] [US7] Verify `BookingNoShowJob` + `markNoShowExpiredConfirmed` uses `atStartOfDay()+noShowAfterHours` in `Backend/src/main/java/com/homestay/job/BookingNoShowJob.java` / `BookingService.java`
- [X] T031 [US7] Confirm `BookingNoShowServiceTest.java` still green after inventory query changes in `Backend/src/test/java/com/homestay/service/BookingNoShowServiceTest.java`

**Checkpoint**: US7 jobs match FR-007/008 clarify

---

## Phase 10: Polish & Cross-Cutting

**Purpose**: Docs, coverage, AGENTS drift, smoke

- [X] T032 [P] Sync stale “known gaps” in root `AGENTS.md` §8 (NoShow/cancel tiers/FR-015 marked Done) to match `sdd/sprints/tech-debt.md`
- [X] T033 [P] Run `Backend` unit tests for booking suite (`Booking*Test`) via Maven/`./mvnw test` with JDK 17
- [X] T034 Run `mvn -Pcoverage-critical verify` in `Backend/` if Auth/Booking/Payment touched
- [X] T035 Execute `sdd/services/homestay/features/004-booking-inventory/quickstart.md` smoke checklist end-to-end
- [X] T036 [P] Optional FE lint: `npm run lint` in `Frontend/`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: Immediate
- **Phase 2 Foundational**: After Setup — **BLOCKS** US sign-off
- **US1–US4, US6–US7**: After T004–T008 (inventory gate); can verify in parallel once foundation done
- **US5**: Documentation only; no code feature
- **Polish**: After desired stories verified

### User Story Dependencies

| Story | Depends on | Notes |
|-------|------------|-------|
| US1 | Phase 2 | MVP; needs half-open + block |
| US2 | US1 data exists | Read-only verify |
| US3 | US1 confirmed path | Check-in/out |
| US4 | US1 confirmed | Cancel tiers |
| US5 | — | Deferred |
| US6 | Phase 2 (T007) | Modify + block |
| US7 | US1 statuses | Jobs |

### Parallel Opportunities

- T001–T003 parallel
- T005 parallel with T004 prep; T009 after T004–T007
- After Phase 2: T013∥T015∥T016∥T020∥T023∥T027∥T029∥T030
- T032∥T036 in polish

### Parallel Example: Phase 2

```text
T004 RoomRepository half-open overlap queries
T005 RoomRepository search EXISTS half-open (parallel file sections / after T004 if same file — do sequential if conflict)
T006+T008 BookingService create + inject repository
T007 BookingService modify block check
T009 tests after T006–T007
```

---

## Implementation Strategy

### MVP (clarify-closeout)

1. Phase 1 → Phase 2 (T004–T010)  
2. US1 verify (T011–T014)  
3. **STOP** — demo create / turnover / block 409 / VNPay confirm  

### Incremental

1. US2–US3 verify  
2. US4 policy copy + day-7 test  
3. US6 modify + block test  
4. US7 job regression  
5. Polish + AGENTS sync  

### Suggested MVP scope

**Phase 2 + US1** only (inventory clarify + create/deposit).

---

## Notes

- Do not implement TD-011 bank transfer / TD-012b IPN / TD-010 reconcile unless Spec Sync reopens  
- Prefer `existsActiveOverlappingStay` (already on `RoomStatusBlockRepository`) over reimplementing inclusive `findActiveOverlappingRange` for stay nights  
- Commit after Phase 2 and after each US checkpoint  
- `/speckit-implement` can execute this list in order  
