# Tasks: FR-10 Contract Management

**Input**: Design documents from `specs/010-contract-management/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/contract-api.yaml, quickstart.md

**Phụ thuộc**: FR-04 (`outbox_events` V023, CONFIRMED + `CONTRACT_GENERATE_REQUESTED`); FR-12 deposit success; FR-01 email; FR-06 manager property scope; FR-08 room snapshot.

**Tests**: Không có phase test riêng per-story. Unit + integration trong Phase Polish per plan Phase K.

**Organization**: Tasks grouped by user story (US1–US4) for independent delivery.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no blocking deps)
- **[Story]**: US1–US4 maps to spec.md user stories

## Path Conventions

- **Backend**: `Backend/src/main/java/com/homestay/`
- **Backend tests**: `Backend/src/test/java/com/homestay/`
- **Frontend**: `Frontend/src/`
- **Migrations**: `Backend/src/main/resources/db/migration/`
- **Contract**: `specs/010-contract-management/contracts/contract-api.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Flyway `contracts` after FR-04 outbox; PDF + mail config

- [X] T001 Verify FR-04 `outbox_events` migration applied (`V023__outbox_and_booking_stubs.sql` or equivalent) per `specs/004-booking-inventory/quickstart.md`
- [X] T002 [P] Confirm Vite proxy `/api/v1` → backend in `Frontend/vite.config.ts`
- [X] T003 Create Flyway `Backend/src/main/resources/db/migration/V024__contracts.sql` — `contracts` table + `UNIQUE (booking_id)` per `data-model.md`
- [X] T004 [P] Add OpenPDF (or PDFBox) dependency in `Backend/pom.xml` per `research.md`
- [X] T005 [P] Add `app.upload.contracts-dir` (env `APP_CONTRACTS_DIR`) and Spring Mail settings in `Backend/src/main/resources/application.yml`
- [X] T006 [P] Create optional Flyway `Backend/src/main/resources/db/migration/V025__contract_addendums.sql` for US4 P2 (skip if deferring addendum)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Contract entity, DTOs, service skeletons, security — MUST complete before user story phases

**⚠️ CRITICAL**: No user story work until this phase complete

- [X] T007 [P] Create `ContractStatus.java` enum in `Backend/src/main/java/com/homestay/enums/ContractStatus.java` — ACTIVE, COMPLETED, CANCELLED
- [X] T008 [P] Create `Contract.java` entity in `Backend/src/main/java/com/homestay/entities/Contract.java` per `data-model.md`
- [X] T009 [P] Create `ContractRepository.java` in `Backend/src/main/java/com/homestay/repositories/ContractRepository.java` — `existsByBookingId`, `findByCustomerId`, `findByPropertyIdIn`
- [X] T010 [P] Create contract DTOs in `Backend/src/main/java/com/homestay/dtos/contract/` — `ContractSummaryResponse`, `ContractDetailResponse`, `ContractPageResponse`, `ResendContractRequest`, `ContractAddendumResponse` per `contracts/contract-api.yaml`
- [X] T011 Create `ContractService.java` skeleton in `Backend/src/main/java/com/homestay/services/ContractService.java`
- [X] T012 Create `ContractPdfService.java` skeleton in `Backend/src/main/java/com/homestay/services/ContractPdfService.java`
- [X] T013 Create `ContractEmailService.java` skeleton in `Backend/src/main/java/com/homestay/services/ContractEmailService.java`
- [X] T014 Create `ContractOutboxWorker.java` skeleton in `Backend/src/main/java/com/homestay/services/ContractOutboxWorker.java` — inject OutboxEventRepository from FR-04
- [X] T015 Register `/api/v1/contracts/me/**` (CUSTOMER) and `/api/v1/manager/contracts/**` (MANAGER) in `Backend/src/main/java/com/homestay/configs/SecurityConfig.java`
- [X] T016 [P] Align TypeScript types in `Frontend/src/api/contractApi.ts` with `contracts/contract-api.yaml` (prepare v1 path migration)

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Tự động tạo và gửi hợp đồng khi đặt cọc (Priority: P1) 🎯 MVP

**Goal**: Outbox worker processes `CONTRACT_GENERATE_REQUESTED` → immutable PDF + email; idempotent per booking

**Independent Test**: Deposit CONFIRMED → within 5 min contract row with pdfUrl ACTIVE + sentAt; email in MailHog; replay does not duplicate

### Implementation

- [X] T017 [US1] Create HTML template `Backend/src/main/resources/templates/contract/accommodation-contract.html` — Vietnamese snapshot fields per spec Assumptions
- [X] T018 [US1] Implement `generatePdf(Booking, User, Room, Property)` saving to `APP_CONTRACTS_DIR` in `ContractPdfService.java`
- [X] T019 [US1] Build snapshot fields (customerName, roomNumber, propertyName, amounts, dates) on Contract insert in `ContractPdfService.java`
- [X] T020 [US1] Implement `handleContractGenerateRequested(bookingId)` with idempotent `existsByBookingId` check in `ContractOutboxWorker.java`
- [X] T021 [US1] Insert Contract row + enqueue `CONTRACT_EMAIL_SEND` outbox after PDF success in `ContractOutboxWorker.java`
- [X] T022 [US1] Implement `sendContractEmail(contractId)` with PDF attachment in `ContractEmailService.java`
- [X] T023 [US1] Implement `handleContractEmailSend(contractId)` updating `sentAt` in `ContractOutboxWorker.java`
- [X] T024 [US1] Add `@Scheduled(fixedDelay = 15000)` poll PENDING outbox events in `ContractOutboxWorker.java` — process CONTRACT_* types only
- [X] T025 [US1] Implement retry policy max 5 attempts then FAILED in `ContractOutboxWorker.java`
- [X] T026 [US1] Log `CONTRACT_GENERATED` to ActivityLog in `ContractOutboxWorker.java` after contract create
- [X] T027 [US1] Log `CONTRACT_EMAIL_SENT` to ActivityLog in `ContractEmailService.java` after successful send
- [X] T028 [US1] Verify FR-04 `OutboxPublisher` emits `CONTRACT_GENERATE_REQUESTED` (not duplicate event name) — align in FR-04 or FR-10 worker handler

**Checkpoint**: US1 MVP — auto contract generation + email pipeline

---

## Phase 4: User Story 2 — Customer xem, tải và in hợp đồng (Priority: P1)

**Goal**: SCR-21 — customer list, detail, PDF download/print; own contracts only

**Independent Test**: Customer `/customer/contracts` → list; drawer PDF; download; other customer → 403

### Implementation

- [X] T029 [US2] Implement `listForCustomer(customerId, filters, pageable)` in `ContractService.java`
- [X] T030 [US2] Implement `getDetailForCustomer(contractId, customerId)` ownership check in `ContractService.java`
- [X] T031 [US2] Implement `getPdfBytes(contractId, requester)` streaming immutable file in `ContractService.java`
- [X] T032 [US2] Implement `getByBookingId(bookingId, customerId)` in `ContractService.java`
- [X] T033 [US2] Create `CustomerContractController.java` with `GET /api/v1/contracts/me`, `/{id}`, `/{id}/pdf`, `/booking/{bookingId}` in `Backend/src/main/java/com/homestay/controllers/CustomerContractController.java`
- [X] T034 [P] [US2] Migrate `getMyContracts` → `GET /api/v1/contracts/me` in `Frontend/src/api/contractApi.ts`
- [X] T035 [P] [US2] Migrate detail/download/booking paths to `/api/v1/contracts/**` in `Frontend/src/api/contractApi.ts`
- [X] T036 [US2] Wire list + filters on `Frontend/src/pages/customer/ContractListPage.tsx`
- [X] T037 [US2] Create `ContractPdfDrawer.tsx` in `Frontend/src/components/contract/ContractPdfDrawer.tsx` — iframe PDF viewer
- [X] T038 [US2] Wire row click to open drawer on `ContractListPage.tsx`
- [X] T039 [US2] Add Download button calling `downloadContractPdf` on `ContractPdfDrawer.tsx`
- [X] T040 [US2] Add Print button invoking browser print on iframe in `ContractPdfDrawer.tsx`
- [X] T041 [US2] Verify `ProtectedRoute role="CUSTOMER"` for `/customer/contracts` routes in `Frontend/src/App.tsx`

**Checkpoint**: US2 testable — Customer SCR-21 complete

---

## Phase 5: User Story 3 — Manager xem hợp đồng và gửi lại email (Priority: P1)

**Goal**: SCR-38 — manager list scoped by property; detail; resend via Outbox without PDF regenerate

**Independent Test**: Manager list filtered by property; resend updates sentAt; out-of-scope → 403

### Implementation

- [X] T042 [US3] Implement `listForManager(managerId, propertyId, filters, pageable)` with PropertyAccessValidator in `ContractService.java`
- [X] T043 [US3] Implement `getDetailForManager(contractId, managerId)` property scope check in `ContractService.java`
- [X] T044 [US3] Implement `requestResend(contractId, managerId, emailOverride)` enqueueing `CONTRACT_RESEND` in `ContractService.java`
- [X] T045 [US3] Implement `handleContractResend(contractId, email)` — same pdfUrl attachment in `ContractOutboxWorker.java`
- [X] T046 [US3] Create `ManagerContractController.java` with `GET /api/v1/manager/contracts`, `/{id}`, `/{id}/pdf`, `POST /{id}/resend` in `Backend/src/main/java/com/homestay/controllers/ManagerContractController.java`
- [X] T047 [US3] Log `CONTRACT_RESENT` to ActivityLog in `ContractEmailService.java` on resend success
- [X] T048 [P] [US3] Migrate manager list to `GET /api/v1/manager/contracts?propertyId=` in `Frontend/src/api/contractApi.ts`
- [X] T049 [US3] Migrate `Frontend/src/pages/manager/ContractMgmtListPage.tsx` — property filter via FR-06 properties API
- [X] T050 [US3] Migrate `Frontend/src/pages/manager/ContractMgmtDetailPage.tsx` to v1 detail + PDF download
- [X] T051 [US3] Migrate `Frontend/src/pages/manager/ResendContractPage.tsx` to `POST /api/v1/manager/contracts/{id}/resend`
- [X] T052 [US3] Reuse `ContractPdfDrawer.tsx` on manager detail with resend action link

**Checkpoint**: US3 testable — Manager SCR-38 complete

---

## Phase 6: User Story 4 — Contract Addendum khi Damage Fee (Priority: P2)

**Goal**: Separate addendum PDF linked to parent; original pdfUrl unchanged

**Independent Test**: Damage approve (when FR-23 ready) → addendum row; parent contract unchanged

### Implementation

- [X] T053 [US4] Create `ContractAddendum.java` entity in `Backend/src/main/java/com/homestay/entities/ContractAddendum.java` per `data-model.md`
- [X] T054 [US4] Create `ContractAddendumRepository.java` in `Backend/src/main/java/com/homestay/repositories/ContractAddendumRepository.java`
- [X] T055 [US4] Implement `generateAddendum(parentContractId, damageFeeAmount)` separate PDF in `ContractPdfService.java`
- [X] T056 [US4] Add handler stub for `CONTRACT_ADDENDUM_REQUESTED` outbox event in `ContractOutboxWorker.java` (wire to FR-23 when available)
- [X] T057 [US4] Include `addendums[]` in `ContractDetailResponse` mapping in `ContractService.java`
- [X] T058 [US4] Display addendum list read-only on `ContractPdfDrawer.tsx` and `ContractMgmtDetailPage.tsx`

**Checkpoint**: US4 testable when FR-23 damage flow exists

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Booking status sync, tests, quickstart validation

- [X] T059 Implement `syncStatusFromBooking(bookingId, bookingStatus)` — CHECKED_OUT→COMPLETED, CANCELLED/NO_SHOW→CANCELLED in `ContractService.java`
- [X] T060 Hook booking status listener or call sync from FR-04 booking service integration point in `ContractService.java`
- [X] T061 [P] Unit test `ContractServiceTest.java` in `Backend/src/test/java/com/homestay/unit/ContractServiceTest.java` — idempotency + ownership + status sync
- [X] T062 [P] Unit test `ContractOutboxWorkerTest.java` in `Backend/src/test/java/com/homestay/unit/ContractOutboxWorkerTest.java` — generate + email handlers mocked
- [X] T063 Integration test `ContractControllerIT.java` in `Backend/src/test/java/com/homestay/integration/ContractControllerIT.java` — customer/manager RBAC + PDF download + resend queue
- [X] T064 [P] Assert resend does not change `pdf_url` column in `ContractControllerIT.java`
- [X] T065 Run curl smoke tests in `specs/010-contract-management/quickstart.md` and fix gaps
- [X] T066 [P] Deprecate legacy `/api/contracts/**` paths in `Frontend/src/api/contractApi.ts` comments if dual-run during migration

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: After FR-04 V023 outbox + bookings
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **US1 (P1)**: After Foundational — **blocks US2/US3** (need contracts to list)
- **US2 (P1)**: After US1 (contract records exist) or parallel backend with seeded contract
- **US3 (P1)**: After US1; parallel with US2 backend
- **US4 (P2)**: After US1 + FR-23 damage approve hook
- **Polish**: After US1–US3 minimum

### User Story Dependencies

| Story | Depends on | Independent test |
|-------|------------|------------------|
| US1 | Foundational + FR-04 outbox | Deposit CONFIRMED → contract + email |
| US2 | US1 (or seed) | Customer SCR-21 list/PDF |
| US3 | US1 (or seed) | Manager SCR-38 list/resend |
| US4 | US1 + FR-23 | Addendum without mutating parent PDF |

### Parallel Opportunities

- Phase 1: T002, T004, T005, T006 parallel
- Phase 2: T007–T010, T016 parallel
- US2: T034, T035 parallel after T033
- US3: T048 parallel with T049
- Polish: T061, T062, T064, T066 parallel

---

## Parallel Example: Foundational Phase

```bash
T007 ContractStatus.java | T008 Contract.java | T009 ContractRepository.java
T010 DTOs package | T016 contractApi.ts types
```

---

## Parallel Example: User Story 2

```bash
# After T033 controller:
T034 contractApi getMyContracts migration
T035 contractApi detail/download migration
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 + Phase 2
2. Phase 3 (US1) — Outbox PDF + email pipeline
3. **STOP and VALIDATE**: quickstart deposit → contract row + MailHog
4. Add US2 Customer UI next

### Incremental Delivery

1. Setup + Foundational
2. US1 → auto generation MVP
3. US2 → Customer SCR-21
4. US3 → Manager SCR-38 + resend
5. US4 → Addendum when FR-23 ready
6. Polish → status sync + tests

### Suggested MVP Scope

**T001–T028** (Setup + Foundational + US1) — Auto contract generation on deposit.

### Full Feature Scope

**T001–T066** — Complete FR-10 including Customer/Manager UI, addendum P2, and integration tests.

---

## Notes

- FR-04 owns `CONTRACT_GENERATE_REQUESTED` write — FR-10 owns worker only
- Do not regenerate PDF on manager resend — same `pdf_url`
- UNIQUE `booking_id` enforces one contract per booking
- MailHog recommended for dev email verification
- Commit after each phase checkpoint


## Implementation notes (2026-07-23)

- P0: `/uploads/contracts/**` denyAll; booking lookup read-only; PDF immutable (no regenerate).
- P1: Outbox `CONTRACT_EMAIL_SEND` after generate; NO_SHOW cancels contract; resend 202; manager path alias; FE property filter + PDF preview.
- US4 Addendum deferred.

