# Tasks: FR-11 Billing Management

**Input**: Design documents from `specs/011-billing-management/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/billing-api.yaml, quickstart.md

**Phụ thuộc**: FR-04 (`bookings` table, create/cancel/modify/timeout jobs); FR-01 (CUSTOMER/MANAGER JWT); FR-06 (manager property scope). **Ranh giới**: FR-12 payment execution (VNPay, verify, receipts); FR-10 contract; FR-23 damage invoice — v1 chỉ DEPOSIT + REMAINING_BALANCE.

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
- **Contract**: `specs/011-billing-management/contracts/billing-api.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Flyway `invoices` after FR-04 bookings; payments.invoice_id column for FR-12

- [X] T001 Verify FR-04 `bookings` migration applied (bookings table + create flow) per `specs/004-booking-inventory/quickstart.md`
- [X] T002 [P] Confirm Vite proxy `/api/v1` → backend in `Frontend/vite.config.ts`
- [X] T003 Create Flyway `Backend/src/main/resources/db/migration/V026__invoices.sql` — `invoices` table + `UNIQUE (booking_id, type)` + indexes per `data-model.md`
- [X] T004 [P] Create Flyway `Backend/src/main/resources/db/migration/V027__payments_invoice_id.sql` — add nullable `invoice_id` FK to `payments` + backfill script stub per `data-model.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Invoice entity, DTOs, service skeletons, security — MUST complete before user story phases

**⚠️ CRITICAL**: No user story work until this phase complete

- [X] T005 [P] Create `InvoiceType.java` enum in `Backend/src/main/java/com/homestay/enums/InvoiceType.java` — DEPOSIT, REMAINING_BALANCE
- [X] T006 [P] Create `InvoiceStatus.java` enum in `Backend/src/main/java/com/homestay/enums/InvoiceStatus.java` — UNPAID, PENDING_PAYMENT, PAID, CANCELLED
- [X] T007 [P] Create `Invoice.java` entity in `Backend/src/main/java/com/homestay/entities/Invoice.java` per `data-model.md`
- [X] T008 [P] Create `InvoiceRepository.java` in `Backend/src/main/java/com/homestay/repositories/InvoiceRepository.java` — `existsByBookingIdAndType`, `findByBookingId`, `findByCustomerId`, `findByPropertyIdAndStatus`
- [X] T009 [P] Create invoice DTOs in `Backend/src/main/java/com/homestay/dtos/invoice/` — `InvoiceSummaryResponse`, `InvoiceBreakdownResponse`, `InvoicePageResponse`, `InvoiceDetailResponse` per `contracts/billing-api.yaml`
- [X] T010 Create `InvoiceIssuanceService.java` skeleton in `Backend/src/main/java/com/homestay/services/InvoiceIssuanceService.java`
- [X] T011 Create `InvoiceQueryService.java` skeleton in `Backend/src/main/java/com/homestay/services/InvoiceQueryService.java`
- [X] T012 Create `InvoiceStatusSyncService.java` skeleton in `Backend/src/main/java/com/homestay/services/InvoiceStatusSyncService.java`
- [X] T013 Create `InvoiceCancellationService.java` skeleton in `Backend/src/main/java/com/homestay/services/InvoiceCancellationService.java`
- [X] T014 Create `InvoiceAdjustmentService.java` skeleton in `Backend/src/main/java/com/homestay/services/InvoiceAdjustmentService.java`
- [X] T015 Register `/api/v1/invoices/me/**` (CUSTOMER) and `/api/v1/manager/invoices/**` (MANAGER) in `Backend/src/main/java/com/homestay/configs/SecurityConfig.java`
- [X] T016 [P] Create `Frontend/src/api/invoiceApi.ts` with TypeScript types aligned to `contracts/billing-api.yaml`

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Tự động tạo hóa đơn cọc và phần còn lại (Priority: P1) 🎯 MVP

**Goal**: Booking Pending Deposit → invoice pair (40% + 60%); idempotent; FR-12 sync hook for payment status

**Independent Test**: Create booking → 2 invoices UNPAID with correct amounts; replay create does not duplicate; payment PAID stub → deposit invoice PAID

### Implementation

- [X] T017 [US1] Implement `issuePair(Booking booking)` creating DEPOSIT + REMAINING_BALANCE rows in `InvoiceIssuanceService.java`
- [X] T018 [US1] Add idempotent guard via `existsByBookingIdAndType` before insert in `InvoiceIssuanceService.java`
- [X] T019 [US1] Set `due_date` — deposit = today, remaining = `check_in_date` — in `InvoiceIssuanceService.java`
- [X] T020 [US1] Denormalize `property_id` from room→property on invoice insert in `InvoiceIssuanceService.java`
- [X] T021 [US1] Wire `InvoiceIssuanceService.issuePair()` into `BookingService.createBooking()` same `@Transactional` in `Backend/src/main/java/com/homestay/services/BookingService.java`
- [X] T022 [US1] Remove auto `Payment DEPOSIT PENDING` insert on booking create from `BookingService.java` per `plan.md` FR-04 adjustment note
- [X] T023 [US1] Implement `markPendingPayment(invoiceId)` UNPAID→PENDING_PAYMENT in `InvoiceStatusSyncService.java`
- [X] T024 [US1] Implement `markPaid(invoiceId, paidAt)` → PAID + update `bookings.paid_amount` cache in `InvoiceStatusSyncService.java`
- [X] T025 [US1] Implement `markFailed(invoiceId)` PENDING_PAYMENT→UNPAID in `InvoiceStatusSyncService.java`
- [X] T026 [US1] Add public method `syncFromPayment(invoiceId, paymentStatus, paidAt)` for FR-12 to call in `InvoiceStatusSyncService.java`
- [X] T027 [US1] Log `INVOICE_ISSUED` to ActivityLog after pair create in `InvoiceIssuanceService.java`
- [X] T028 [US1] Log `INVOICE_PAID` to ActivityLog in `InvoiceStatusSyncService.java` on markPaid

**Checkpoint**: US1 MVP — invoice pair auto-created on booking; sync interface ready for FR-12

---

## Phase 4: User Story 2 — Customer xem trạng thái hóa đơn (Priority: P1)

**Goal**: SCR-18 Payment Breakdown + SCR-26 invoice history; customer scope only

**Independent Test**: Customer opens booking detail → deposit/remaining rows with badges; Payment History lists invoices; other customer → 403

### Implementation

- [X] T029 [US2] Implement `listForCustomer(customerId, status, pageable)` in `InvoiceQueryService.java`
- [X] T030 [US2] Implement `getDetailForCustomer(invoiceId, customerId)` ownership check in `InvoiceQueryService.java`
- [X] T031 [US2] Implement `buildBreakdownForBooking(bookingId, customerId)` returning `InvoiceBreakdownResponse` in `InvoiceQueryService.java`
- [X] T032 [US2] Add `isOverdue` computed field in invoice DTO mapper in `InvoiceQueryService.java`
- [X] T033 [US2] Create `CustomerInvoiceController.java` with `GET /api/v1/invoices/me` and `GET /api/v1/invoices/me/{id}` in `Backend/src/main/java/com/homestay/controllers/CustomerInvoiceController.java`
- [X] T034 [US2] Extend `GET /api/v1/bookings/me/{id}` response with `invoiceBreakdown` in `Backend/src/main/java/com/homestay/controllers/CustomerBookingController.java`
- [X] T035 [P] [US2] Implement `getMyInvoices(params)` calling `GET /api/v1/invoices/me` in `Frontend/src/api/invoiceApi.ts`
- [X] T036 [P] [US2] Extend `BookingDetailResponse` type with `invoiceBreakdown` in `Frontend/src/api/bookingApi.ts`
- [X] T037 [US2] Migrate Payment Breakdown section to `invoiceBreakdown.invoices[]` in `Frontend/src/pages/customer/BookingPages.tsx` (SCR-18 detail)
- [X] T038 [US2] Migrate Payment Breakdown to invoices in `Frontend/src/pages/customer/BookingDetailPage.tsx`
- [X] T039 [US2] Wire `PaymentHistoryPage` to `invoiceApi.getMyInvoices` in `Frontend/src/pages/customer/PaymentHistoryPage.tsx`
- [X] T040 [US2] Wire `PaymentHistoryPage` export in `Frontend/src/pages/customer/PaymentPages.tsx` to `invoiceApi` if still used by routes
- [X] T041 [US2] Add fallback read from legacy `payments[]` when `invoiceBreakdown` absent in `BookingPages.tsx`

**Checkpoint**: US2 testable — Customer SCR-18 + SCR-26 complete

---

## Phase 5: User Story 3 — Manager theo dõi hóa đơn theo Property (Priority: P1)

**Goal**: SCR-35 Payment Breakdown + SCR-36 unpaid invoice filter; property scope

**Independent Test**: Manager booking detail shows breakdown; manager invoices list filtered by propertyId + UNPAID; out-of-scope → 403

### Implementation

- [X] T042 [US3] Implement `listForManager(managerId, propertyId, status, pageable)` with PropertyAccessValidator in `InvoiceQueryService.java`
- [X] T043 [US3] Implement `buildBreakdownForManagerBooking(bookingId, managerId)` property scope check in `InvoiceQueryService.java`
- [X] T044 [US3] Create `ManagerInvoiceController.java` with `GET /api/v1/manager/invoices?propertyId=&status=` in `Backend/src/main/java/com/homestay/controllers/ManagerInvoiceController.java`
- [X] T045 [US3] Extend `GET /api/v1/manager/bookings/{id}` response with `invoiceBreakdown` in `Backend/src/main/java/com/homestay/controllers/ManagerBookingController.java`
- [X] T046 [P] [US3] Implement `getManagerInvoices(params)` in `Frontend/src/api/invoiceApi.ts`
- [X] T047 [US3] Add Payment Breakdown card (deposit + remaining status badges) on `Frontend/src/pages/manager/BookingMgmtDetailPage.tsx`
- [X] T048 [US3] Add Unpaid/Pending tab calling `GET /manager/invoices?status=UNPAID` on `Frontend/src/pages/manager/PaymentMgmtListPage.tsx`
- [X] T049 [US3] Wire property selector filter on `PaymentMgmtListPage.tsx` via FR-06 properties API
- [X] T050 [US3] Map invoice status to badge CSS (UNPAID, PENDING_PAYMENT, PAID, CANCELLED, overdue) on `PaymentMgmtListPage.tsx` and `BookingMgmtDetailPage.tsx`

**Checkpoint**: US3 testable — Manager SCR-35 + SCR-36 invoice views complete

---

## Phase 6: User Story 4 — Đồng bộ hóa đơn khi booking thay đổi (Priority: P2)

**Goal**: Cancel/timeout → cancel unpaid invoices; modify → adjust remaining amount; deposit Paid immutable

**Independent Test**: Cancel booking → unpaid invoices CANCELLED, paid deposit stays PAID; modify Confirmed booking → remaining amount updated; ActivityLog events

### Implementation

- [X] T051 [US4] Implement `cancelUnpaidForBooking(bookingId)` — UNPAID/PENDING_PAYMENT→CANCELLED in `InvoiceCancellationService.java`
- [X] T052 [US4] Hook `InvoiceCancellationService` into customer `PATCH /bookings/{id}/cancel` in `BookingService.java`
- [X] T053 [US4] Hook cancellation into manager-initiated cancel in `BookingService.java`
- [X] T054 [US4] Hook cancellation into hold-timeout job in `Backend/src/main/java/com/homestay/jobs/BookingHoldTimeoutJob.java` (or equivalent FR-04 job)
- [X] T055 [US4] Implement `adjustOnBookingModify(booking)` — update remaining invoice amount when deposit already PAID in `InvoiceAdjustmentService.java`
- [X] T056 [US4] Implement adjust both invoices when deposit still UNPAID and TotalAmount changed in `InvoiceAdjustmentService.java`
- [X] T057 [US4] Hook `InvoiceAdjustmentService` into booking modify flow in `BookingService.java`
- [X] T058 [US4] Log `INVOICE_CANCELLED` to ActivityLog in `InvoiceCancellationService.java`
- [X] T059 [US4] Log `INVOICE_ADJUSTED` to ActivityLog in `InvoiceAdjustmentService.java`

**Checkpoint**: US4 testable — invoice lifecycle aligned with booking cancel/modify

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Tests, quickstart validation, route verification

- [X] T060 [P] Unit test idempotency + amount snapshot in `Backend/src/test/java/com/homestay/unit/InvoiceIssuanceServiceTest.java`
- [X] T061 [P] Unit test payment sync transitions in `Backend/src/test/java/com/homestay/unit/InvoiceStatusSyncServiceTest.java`
- [X] T062 [P] Unit test modify/cancel adjustment rules in `Backend/src/test/java/com/homestay/unit/InvoiceAdjustmentServiceTest.java`
- [X] T063 Integration test customer/manager RBAC + breakdown in `Backend/src/test/java/com/homestay/integration/InvoiceControllerIT.java`
- [X] T064 [P] Assert deposit invoice amount immutable after PAID in `InvoiceControllerIT.java`
- [X] T065 Run curl smoke tests in `specs/011-billing-management/quickstart.md` and fix gaps
- [X] T066 [P] Verify customer/manager routes for SCR-18/26/35/36 in `Frontend/src/App.tsx`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: After FR-04 bookings migration
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **US1 (P1)**: After Foundational — **blocks US2/US3** (need invoices to display)
- **US2 (P1)**: After US1 (invoice records exist) or parallel backend with seeded booking+invoices
- **US3 (P1)**: After US1; parallel with US2 backend
- **US4 (P2)**: After US1; can parallel with US2/US3 once issuance works
- **Polish**: After US1–US3 minimum

### User Story Dependencies

| Story | Depends on | Independent test |
|-------|------------|------------------|
| US1 | Foundational + FR-04 booking create | Booking create → 2 invoices UNPAID |
| US2 | US1 (or seed) | Customer SCR-18 breakdown + SCR-26 list |
| US3 | US1 (or seed) | Manager SCR-35 breakdown + SCR-36 filter |
| US4 | US1 | Cancel/modify sync invoice state |

### Parallel Opportunities

- Phase 1: T002, T004 parallel
- Phase 2: T005–T009, T016 parallel
- US2 frontend: T035, T036 parallel after T033–T034
- US3 frontend: T046 parallel after T044–T045
- Polish: T060–T062, T064, T066 parallel

### Parallel Example: User Story 2

```bash
# After T033–T034 backend endpoints ready:
Task T035: "Implement getMyInvoices in Frontend/src/api/invoiceApi.ts"
Task T036: "Extend BookingDetailResponse in Frontend/src/api/bookingApi.ts"

# Frontend pages (sequential per file, different devs):
Task T037: "BookingPages.tsx breakdown"
Task T039: "PaymentHistoryPage.tsx wire invoiceApi"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Create booking → verify 2 invoices in DB + quickstart curl
5. Demo invoice issuance before UI

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 → Invoice auto-issue (MVP backend)
3. US2 → Customer visibility (SCR-18, SCR-26)
4. US3 → Manager visibility (SCR-35, SCR-36)
5. US4 → Lifecycle sync on cancel/modify
6. Polish → Tests + quickstart

### Parallel Team Strategy

1. Team completes Setup + Foundational together
2. Once Foundational done:
   - Developer A: US1 backend (issuance + sync)
   - Developer B: US2 frontend (after US1 API stub/seed)
   - Developer C: US3 manager views
3. US4 after US1 merge; US2/US3 can proceed in parallel

---

## Notes

- FR-12 owns Payment rows — FR-11 only exposes `InvoiceStatusSyncService.syncFromPayment()`; do not implement VNPay in FR-11 tasks
- Remove FR-04 auto Payment insert (T022) before merge to avoid dual deposit tracking
- `[P]` tasks = different files, no dependencies on incomplete tasks in same phase
- Stop at any checkpoint to validate story independently
- Backend scaffold may not exist yet — tasks assume paths from `plan.md`; create package structure as part of T001 if needed
