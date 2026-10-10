# Tasks: FR-12 Payment Management & Reconciliation

**Input**: Design documents from `specs/012-payment-management/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/payment-api.yaml, quickstart.md

**Phụ thuộc**: FR-11 (`invoices` V026, `InvoiceStatusSyncService`, V027 `invoice_id`); FR-04 (`DepositConfirmationService`, booking lifecycle); FR-01 (JWT); FR-06 (property scope); FR-10 (contract Outbox via deposit confirm); FR-15 (notifications). **Ranh giới**: FR-11 Invoice; FR-12 Payment/VNPay/verify/reconciliation; FR-23 damage trigger — US6 P2.

**Tests**: Không có phase test riêng per-story. Unit + integration trong Phase Polish per plan Phase K.

**Organization**: Tasks grouped by user story (US1–US6) for independent delivery.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no blocking deps)
- **[Story]**: US1–US6 maps to spec.md user stories

## Path Conventions

- **Backend**: `Backend/src/main/java/com/homestay/`
- **Backend tests**: `Backend/src/test/java/com/homestay/`
- **Frontend**: `Frontend/src/`
- **Migrations**: `Backend/src/main/resources/db/migration/`
- **Contract**: `specs/012-payment-management/contracts/payment-api.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Payments schema extensions, VNPay env, receipt upload dir — after FR-11 invoices

- [ ] T001 Verify FR-11 `invoices` table + `InvoiceStatusSyncService` available per `specs/011-billing-management/quickstart.md` (blocker)
- [ ] T002 Verify or create Flyway `Backend/src/main/resources/db/migration/V027__payments_invoice_id.sql` — `invoice_id` FK on `payments` per `data-model.md`
- [ ] T003 Create Flyway `Backend/src/main/resources/db/migration/V028__payments_fr12_extensions.sql` — `property_id`, `reconciliation_status`, `reminder_sent_at`, gateway columns + indexes per `data-model.md`
- [x] T004 [P] Add VNPay env placeholders (`VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, URLs) in `Backend/src/main/resources/application.yml` per `quickstart.md`
- [ ] T005 [P] Add `app.upload.receipts-dir` (env `APP_RECEIPTS_DIR`) and confirm Vite proxy `/api/v1` in `Frontend/vite.config.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Payment entities, DTOs, VNPay config, service skeletons — MUST complete before user story phases

**⚠️ CRITICAL**: No user story work until this phase complete

- [ ] T006 [P] Create `PaymentType.java` in `Backend/src/main/java/com/homestay/enums/PaymentType.java` — DEPOSIT, REMAINING_BALANCE, DAMAGE_FEE, REFUND
- [ ] T007 [P] Create `PaymentMethod.java` in `Backend/src/main/java/com/homestay/enums/PaymentMethod.java` — VNPAY, BANK_TRANSFER, CASH
- [ ] T008 [P] Create `PaymentStatus.java` in `Backend/src/main/java/com/homestay/enums/PaymentStatus.java` — PENDING, PAID, FAILED, REFUNDED
- [ ] T009 [P] Create `ReconciliationStatus.java` in `Backend/src/main/java/com/homestay/enums/ReconciliationStatus.java` — NONE, DISCREPANCY, RESOLVED
- [ ] T010 [P] Create `Payment.java` entity in `Backend/src/main/java/com/homestay/entities/Payment.java` per `data-model.md`
- [ ] T011 [P] Create `PaymentReceipt.java` entity in `Backend/src/main/java/com/homestay/entities/PaymentReceipt.java` per `data-model.md`
- [x] T012 [P] Create `PaymentRepository.java` in `Backend/src/main/java/com/homestay/repositories/PaymentRepository.java` — `findByOrderRef`, `findByInvoiceIdAndStatus`, `findByCustomerId`, `findByPropertyIdAndStatus`
- [ ] T013 [P] Create `PaymentReceiptRepository.java` in `Backend/src/main/java/com/homestay/repositories/PaymentReceiptRepository.java`
- [ ] T014 [P] Create payment DTOs in `Backend/src/main/java/com/homestay/dtos/payment/` — `PaymentSummaryResponse`, `PaymentDetailResponse`, `PaymentPageResponse`, `VerifyPaymentRequest`, `CreateVNPayUrlResponse`, `UploadReceiptRequest` per `contracts/payment-api.yaml`
- [ ] T015 Create `VNPayConfig.java` in `Backend/src/main/java/com/homestay/configs/VNPayConfig.java` binding env properties
- [ ] T016 Create `PaymentService.java` skeleton in `Backend/src/main/java/com/homestay/services/PaymentService.java`
- [ ] T017 Create `VNPayService.java` skeleton in `Backend/src/main/java/com/homestay/services/VNPayService.java`
- [ ] T018 Create `PaymentConfirmationService.java` skeleton in `Backend/src/main/java/com/homestay/services/PaymentConfirmationService.java`
- [ ] T019 Create `PaymentVerificationService.java` skeleton in `Backend/src/main/java/com/homestay/services/PaymentVerificationService.java`
- [ ] T020 Register payment routes + permitAll `/api/payments/vnpay/return` in SecurityConfig — ~~`/vnpay/ipn`~~ **DEFERRED TD-012**

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Customer thanh toán đặt cọc (VNPay / chuyển khoản) (Priority: P1) 🎯 MVP

**Goal**: Deposit pay via **VNPay only**; confirmPaid → booking Confirmed (+ invoice/contract when ready)

**Independent Test**: VNPay deposit success → Payment Paid, booking Confirmed

### Implementation

- [ ] T021 [US1] Implement `createPaymentAttempt(invoiceId, method)` — validate invoice DEPOSIT UNPAID, amount match, one Pending per invoice in `PaymentService.java`
- [x] T022 [US1] Generate unique `orderRef` and persist Payment PENDING in `PaymentService.java`
- [ ] T023 [US1] Call `InvoiceStatusSyncService.markPendingPayment(invoiceId)` on payment create in `PaymentService.java`
- [ ] T024 [US1] Implement HMAC-SHA512 `buildPaymentUrl(Payment)` in `VNPayService.java`
- [ ] T025 [US1] Implement `verifyIpnSignature(Map params)` in `VNPayService.java`
- [ ] T026 [US1] Implement `confirmPaid(Payment)` idempotent — Paid, invoice sync, deposit → `DepositConfirmationService.confirmDeposit()` in `PaymentConfirmationService.java`
- [ ] T027 [US1] Implement `markFailed(Payment, note)` — Failed + invoice revert in `PaymentConfirmationService.java`
- [x] T028 [US1] Create `PaymentVNPayController.java` with `POST /api/v1/payments/vnpay/create-url?bookingId&type=DEPOSIT` in `Backend/src/main/java/com/homestay/controllers/PaymentVNPayController.java`
- [ ] ~~T029 [US1] Add `POST /api/v1/payments/vnpay/ipn` handler~~ **DEFERRED TD-012**
- [ ] T030 [US1] Add `GET /api/v1/payments/vnpay/return` redirect to frontend result page in `PaymentVNPayController.java`
- [ ] ~~T031 [US1] PaymentReceiptService bank transfer upload~~ **DEFERRED TD-011**
- [ ] ~~T032 [US1] CustomerReceiptController SCR-20~~ **DEFERRED TD-011**
- [x] T033 [US1] Log `PAYMENT_CREATED` and `PAYMENT_PAID` to ActivityLog in `PaymentConfirmationService.java`
- [x] T034 [P] [US1] Migrate `createVnpayUrl` to `POST /api/v1/payments/vnpay/create-url` in `Frontend/src/api/paymentApi.ts`
- [ ] T035 [US1] Wire `DepositPaymentPage` **VNPay-only** redirect (remove bank transfer UI) in customer payment pages

**Checkpoint**: US1 MVP — deposit **VNPay only**

---

## Phase 4: User Story 2 — Customer thanh toán phần còn lại (Priority: P1)

**Goal**: Remaining balance pay when booking Confirmed; no duplicate deposit confirm side-effects

**Independent Test**: Confirmed booking → pay remaining → Payment Paid + remaining invoice Paid; reject if not Confirmed

### Implementation

- [x] T036 [US2] Add booking status gate — only **Confirmed** for REMAINING_BALANCE in `PaymentService.createPaymentAttempt()` in `PaymentService.java`
- [x] T037 [US2] Extend `POST /api/v1/payments/vnpay/create-url?type=REMAINING_BALANCE` validation in `PaymentVNPayController.java`
- [x] T038 [US2] Ensure `confirmPaid` for REMAINING skips `DepositConfirmationService` — only updates invoice + `paid_amount` in `PaymentConfirmationService.java`
- [ ] ~~T039 [US2] PaymentReceiptService remaining bank transfer~~ **DEFERRED TD-011**
- [ ] T040 [P] [US2] Wire `RemainingPaymentPage` to v1 create-url (**VNPay only**) in customer payment pages
- [ ] T041 [US2] Update `VNPayResultPage` to handle remaining payment success messaging

**Checkpoint**: US2 testable — remaining **VNPay only**

---

## Phase 5: User Story 3 — Manager xác minh chuyển khoản

> **CANCELLED / DEFERRED for edu (2026-07-23) — TD-011.** Do **not** implement T042–T052 bank-transfer verify / receipt / reminder.  
> Manager payment **list/detail (read)** may remain under US5 — without Approve CK.

**Goal**: ~~SCR-37 verify CK + receipt required~~ *(out of scope)*

### Implementation

- [ ] ~~T042–T052 [US3] Manager verify / receipt / reminder jobs & UI~~ **DEFERRED TD-011**

**Checkpoint**: ~~US3~~ skipped for edu (VNPay-only)

---

## Phase 6: User Story 4 — VNPay reconciliation cron + Admin SCR-52

> **CANCELLED / DEFERRED for edu (2026-07-23) — TD-010.** Do **not** implement T053–T060 unless Spec Sync reopens.  
> Still required elsewhere: VNPay create-url + signature verify on **Return URL** — see US4 scenarios 2 & 5 in `spec.md`.  
> ~~IPN~~ **DEFERRED TD-012** — do not implement IPN handlers unless Spec Sync reopens.

**Goal**: ~~Cron 15 min query Pending VNPay; Admin discrepancy list + manual sync~~ *(out of scope)*

### Implementation

- [ ] ~~T053 [US4] Implement `queryTransaction(orderRef)` VNPay querydr API~~ **DEFERRED TD-010**
- [ ] ~~T054 [US4] Create `PaymentReconciliationJob.java`~~ **DEFERRED TD-010**
- [ ] ~~T055 [US4] On amount mismatch set `reconciliation_status = DISCREPANCY`~~ **DEFERRED TD-010**
- [ ] ~~T056 [US4] Create `AdminReconciliationService.java`~~ **DEFERRED TD-010**
- [ ] ~~T057 [US4] Create `AdminReconciliationController.java`~~ **DEFERRED TD-010**
- [ ] ~~T058 [US4] Log `PAYMENT_RECONCILED`~~ **DEFERRED TD-010**
- [ ] ~~T059 [US4] Create `PaymentReconciliationPage.tsx` SCR-52~~ **DEFERRED TD-010**
- [ ] ~~T060 [US4] Register `/admin/reconciliation`~~ **DEFERRED TD-010**

**Checkpoint**: ~~US4 reconciliation~~ skipped for edu

---

## Phase 7: User Story 5 — Payment history Customer & Manager (Priority: P1)

**Goal**: SCR-26 customer history + manager detail with receipt link

**Independent Test**: Customer GET /payments/me own only; detail shows receiptUrl and verify fields

### Implementation

- [x] T061 [US5] Implement `listForCustomer(customerId, pageable)` in `PaymentService.java`
- [x] T062 [US5] Implement `getDetailForCustomer(paymentId, customerId)` ownership check in `PaymentService.java`
- [x] T063 [US5] Create `CustomerPaymentController.java` with `GET /api/v1/payments/me` and `GET /api/v1/payments/me/{id}` in `Backend/src/main/java/com/homestay/controllers/CustomerPaymentController.java`
- [x] T064 [US5] Map `receiptUrl`, `verifiedByName`, `reconciliationStatus` in PaymentDetailResponse mapper in `PaymentService.java`
- [x] T065 [P] [US5] Add `getMyPayments` and `getMyPaymentDetail` to `Frontend/src/api/paymentApi.ts`
- [ ] T066 [US5] Wire `PaymentHistoryPage` to live API in `Frontend/src/pages/customer/PaymentHistoryPage.tsx`
- [ ] T067 [US5] Wire duplicate export in `Frontend/src/pages/customer/PaymentPages.tsx` PaymentHistory section if still used

**Checkpoint**: US5 testable — payment history SCR-26 live

---

## Phase 8: User Story 6 — Damage Fee Payment (Priority: P2)

**Goal**: Stub listener for FR-23 damage approve → damage invoice pay path

**Independent Test**: When FR-23 fires event, listener creates pay-eligible damage invoice reference (stub ok v1)

### Implementation

- [ ] T068 [US6] Create `DamagePaymentListener.java` stub listening for damage-approved event in `Backend/src/main/java/com/homestay/listeners/DamagePaymentListener.java`
- [ ] T069 [US6] Add `createPaymentAttempt` support for DAMAGE_FEE invoice type when booking PENDING_DAMAGE_PAYMENT in `PaymentService.java`
- [ ] T070 [US6] Document FR-23 integration contract in comment block on `DamagePaymentListener.java`

**Checkpoint**: US6 stub ready — full flow when FR-23 ships

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Tests, quickstart validation, security hardening

- [ ] T071 [P] Unit test VNPay signature verify/build in `Backend/src/test/java/com/homestay/unit/VNPayServiceTest.java`
- [ ] T072 [P] Unit test confirmPaid idempotency + deposit vs remaining side-effects in `Backend/src/test/java/com/homestay/unit/PaymentConfirmationServiceTest.java`
- [ ] T073 [P] Unit test approve-without-receipt rejection in `Backend/src/test/java/com/homestay/unit/PaymentVerificationServiceTest.java`
- [ ] T074 Integration test IPN + manager verify RBAC in `Backend/src/test/java/com/homestay/integration/PaymentControllerIT.java`
- [ ] T075 [P] Assert no double booking Confirmed on IPN replay in `PaymentControllerIT.java`
- [ ] T076 Run curl smoke tests in `specs/012-payment-management/quickstart.md` and fix gaps
- [ ] T077 [P] Verify all payment routes in `Frontend/src/App.tsx` (customer/manager/admin)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: After FR-11 invoices (blocker)
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **US1 (P1)**: After Foundational — **blocks US2–US5** (core payment flow)
- **US2 (P1)**: After US1 (shared PaymentService/VNPay)
- **US3 (P1)**: After US1 (bank transfer payments exist)
- **US4 (P1)**: After US1 (VNPay Pending payments exist); parallel with US3/US5
- **US5 (P1)**: After US1 (payment records exist); parallel with US3/US4
- **US6 (P2)**: After US1; depends FR-23 for full test
- **Polish**: After US1–US5 minimum

### User Story Dependencies

| Story | Depends on | Independent test |
|-------|------------|------------------|
| US1 | Foundational + FR-11 | VNPay/bank deposit pay |
| US2 | US1 | Remaining pay on Confirmed booking |
| US3 | US1 | Manager verify bank transfer |
| US4 | US1 | Cron + admin reconciliation |
| US5 | US1 | Customer/manager payment history |
| US6 | US1 + FR-23 | Damage fee pay stub |

### Parallel Opportunities

- Phase 1: T004, T005 parallel
- Phase 2: T006–T014 parallel
- US1 frontend: T034 parallel after T028–T032
- US3 frontend: T049 parallel after T045
- US5: T065 parallel after T063
- Polish: T071–T073, T075, T077 parallel

### Parallel Example: User Story 1

```bash
# After backend endpoints T028–T032:
Task T034: "Migrate paymentApi.ts to /api/v1"
Task T035: "Wire DepositPaymentPage in PaymentPages.tsx"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (verify FR-11 blocker)
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: VNPay sandbox deposit + bank transfer Pending per `quickstart.md`
5. Demo deposit → Confirmed booking path

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 → Deposit pay MVP (VNPay + bank)
3. US2 → Remaining balance
4. US3 → Manager verify + reminders
5. US4 → Reconciliation cron + Admin SCR-52
6. US5 → Payment history UI
7. US6 → Damage fee stub (P2)
8. Polish → Tests + quickstart

### Parallel Team Strategy

1. Team completes Setup + Foundational (after FR-11 merged)
2. Once Foundational done:
   - Developer A: US1 backend (VNPay + confirmPaid)
   - Developer B: US1 frontend + US5 history
   - Developer C: US3 manager verify
3. US4 after US1 VNPay Pending exists

---

## Notes

- **Blocker**: FR-11 US1 must ship before FR-12 payment create (invoices required)
- `DepositConfirmationService` from FR-04 — wire or stub per `specs/004-booking-inventory/tasks.md` T068
- Never log `VNPAY_HASH_SECRET`; IPN must verify signature before confirmPaid
- Frontend legacy POST verify — migrate to PATCH APPROVED/REJECTED per api-spec
- Stop at any checkpoint to validate story independently
