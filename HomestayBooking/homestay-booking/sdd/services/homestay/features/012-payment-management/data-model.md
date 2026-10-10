# Data Model: FR-12 Payment Management & Reconciliation

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: `docs/Specification_v2.md` §5 Payment/PaymentReceipt, FR-04/FR-11 data-models

## Scope

FR-12 **owns** `payments`, `payment_receipts`, VNPay integration, verification, reconciliation jobs. **Consumes** FR-11 `invoices` (via `invoice_id`). **Triggers** FR-11 `InvoiceStatusSyncService`, FR-04 `DepositConfirmationService`, FR-10 contract Outbox on deposit Paid.

## ERD

```text
Invoice 1──* Payment 1──* PaymentReceipt
Booking 1──* Payment
Customer 1──* Payment
Property (denorm) ── manager/admin filter

VNPay IPN / Cron ──> PaymentConfirmationService ──> Invoice + Booking + Outbox
Manager verify ──> PaymentVerificationService ──> same
```

## Table: payments (FR-12 full owner)

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| invoice_id | UUID | FK invoices NOT NULL (V027) | FR-11 |
| booking_id | UUID | FK bookings NOT NULL | |
| customer_id | UUID | FK users NOT NULL | |
| property_id | UUID | FK properties NOT NULL | Denormalized |
| type | VARCHAR(20) | NOT NULL | DEPOSIT, REMAINING_BALANCE, DAMAGE_FEE, REFUND |
| amount | DECIMAL(15,2) | NOT NULL | Must match invoice at create |
| method | VARCHAR(16) | NOT NULL | VNPAY, BANK_TRANSFER, CASH |
| status | VARCHAR(16) | NOT NULL DEFAULT 'PENDING' | PENDING, PAID, FAILED, REFUNDED |
| order_ref | VARCHAR(64) | UNIQUE NOT NULL | VNPay vnp_TxnRef |
| gateway_transaction_id | VARCHAR(64) | nullable | VNPay transaction no |
| gateway_response_code | VARCHAR(16) | nullable | VNPay response code |
| ipn_received_at | TIMESTAMPTZ | nullable | |
| reconciliation_status | VARCHAR(16) | DEFAULT 'NONE' | NONE, DISCREPANCY, RESOLVED |
| verified_by | UUID | FK users nullable | Manager |
| verified_at | TIMESTAMPTZ | nullable | |
| verification_note | TEXT | nullable | |
| reminder_sent_at | TIMESTAMPTZ | nullable | 24h bank transfer reminder |
| paid_at | TIMESTAMPTZ | nullable | |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**Indexes**:

- `UNIQUE (order_ref)`
- `(invoice_id, status)` — one active PENDING per invoice (app enforced)
- `(customer_id, created_at DESC)` — SCR-26
- `(property_id, status, created_at DESC)` — SCR-36
- `(method, status, created_at)` — reconciliation cron
- `(reconciliation_status)` WHERE DISCREPANCY — SCR-52

### PaymentType enum

`DEPOSIT` | `REMAINING_BALANCE` | `DAMAGE_FEE` | `REFUND`

v1 implements DEPOSIT + REMAINING_BALANCE; DAMAGE_FEE P2 with FR-23.

### PaymentMethod enum

`VNPAY` | `BANK_TRANSFER` | `CASH`

### PaymentStatus enum

`PENDING` | `PAID` | `FAILED` | `REFUNDED`

### Status transitions

```text
(create pay attempt)           → PENDING
VNPay IPN/cron/verify success → PAID (+ paid_at, side effects)
VNPay fail / Manager reject    → FAILED
Refund (cancel policy)         → REFUNDED (v1 record only)
```

## Table: payment_receipts

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| payment_id | UUID | FK payments NOT NULL | |
| file_url | VARCHAR(512) | NOT NULL | |
| file_name | VARCHAR(255) | NOT NULL | |
| file_size | BIGINT | NOT NULL | |
| created_at | TIMESTAMPTZ | NOT NULL | |

**Indexes**: `(payment_id)` — at least one required before Manager APPROVE bank transfer.

## Flyway

```text
V027__payments_invoice_id.sql     # FR-11/FR-12 shared — invoice_id FK
V028__payments_fr12_extensions.sql  # property_id, reconciliation_status, reminder_sent_at, ipn columns if missing
```

If FR-04 already created bare `payments` table, V028 adds missing columns + indexes.

## API DTOs

### PaymentSummaryResponse

```json
{
  "id": "uuid",
  "bookingId": "uuid",
  "invoiceId": "uuid",
  "type": "DEPOSIT",
  "method": "VNPAY",
  "amount": 400000,
  "status": "PAID",
  "orderRef": "abc-123",
  "paidAt": "2026-06-27T10:00:00Z",
  "createdAt": "2026-06-27T09:55:00Z"
}
```

### PaymentDetailResponse

Extends summary + `customerName`, `verifiedByName`, `verifiedAt`, `verificationNote`, `receiptUrl`, `reconciliationStatus`, `gatewayTransactionId`.

### VerifyPaymentRequest

```json
{ "status": "APPROVED", "note": "Matched bank statement" }
```

### CreateVNPayUrlResponse

```json
{ "paymentId": "uuid", "paymentUrl": "https://sandbox.vnpayment.vn/..." }
```

## ActivityLog Events

| Event | When |
|-------|------|
| PAYMENT_CREATED | Payment PENDING created |
| PAYMENT_PAID | confirmPaid success |
| PAYMENT_FAILED | reject / VNPay fail |
| PAYMENT_VERIFIED | Manager approve |
| PAYMENT_REJECTED | Manager reject |
| PAYMENT_RECONCILED | Cron or admin sync |

## Integration Points

| Feature | Integration |
|---------|-------------|
| FR-11 | Read invoice; InvoiceStatusSyncService on lifecycle |
| FR-04 | DepositConfirmationService on deposit PAID |
| FR-10 | Outbox via FR-04 confirmDeposit |
| FR-06 | Manager property scope |
| FR-15 | Notification on bank pending + 24h reminder |
| FR-17 | Bank account info for SCR-20 display |
| FR-23 | Damage fee payment P2 |

## Security

| Endpoint | CUSTOMER | MANAGER | ADMIN |
|----------|----------|---------|-------|
| POST /payments/vnpay/create-url | Own booking | — | — |
| GET /payments/me | Own | — | R |
| POST /bookings/{id}/receipts | Own | — | — |
| GET /manager/payments | — | Property scope | R |
| PATCH /manager/payments/{id}/verify | — | Property scope | — |
| GET /admin/payments/reconciliation | — | — | Yes |
| POST /admin/payments/{id}/sync-vnpay | — | — | Yes |
| POST /payments/vnpay/ipn | Public (signed) | Public | Public |

## Service Layer

| Service | Responsibility |
|---------|----------------|
| `PaymentService` | create attempt, list, detail |
| `VNPayService` | URL, IPN verify, querydr |
| `PaymentConfirmationService` | confirmPaid idempotent side-effects |
| `PaymentVerificationService` | manager approve/reject |
| `PaymentReceiptService` | upload, link to payment |
| `PaymentReconciliationJob` | cron 15 min |
| `PaymentReminderJob` | 24h bank transfer reminder |
| `AdminReconciliationService` | SCR-52 list + manual sync |
