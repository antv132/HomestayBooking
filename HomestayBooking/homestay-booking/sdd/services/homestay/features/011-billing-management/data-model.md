# Data Model: FR-11 Billing Management

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: `docs/Specification_v2.md` §5 Booking/Payment, FR-04 data-model

## Scope

FR-11 **owns** `invoices` table and invoice lifecycle services. **Consumes** booking create/modify/cancel events from FR-04. **Publishes** sync hooks for FR-12 Payment status updates. Does **not** own `payments`, `payment_receipts`, VNPay, or receipt upload.

## ERD

```text
Booking 1──* Invoice 1──* Payment (FR-12, invoice_id FK)
Customer 1──* Invoice
Property (denorm) ── filter Manager scope

Booking create/modify/cancel ──triggers──> InvoiceIssuance / Adjustment / Cancellation
Payment status change (FR-12) ──triggers──> InvoiceStatusSyncService
```

## Table: invoices

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| booking_id | UUID | FK bookings NOT NULL | |
| customer_id | UUID | FK users NOT NULL | |
| property_id | UUID | FK properties NOT NULL | Denormalized from room→property |
| type | VARCHAR(20) | NOT NULL | DEPOSIT, REMAINING_BALANCE |
| amount | DECIMAL(15,2) | NOT NULL | Snapshot from booking at issue/adjust |
| status | VARCHAR(20) | NOT NULL DEFAULT 'UNPAID' | See enum below |
| due_date | DATE | NOT NULL | Deposit: create date; Remaining: check_in_date |
| paid_at | TIMESTAMPTZ | nullable | Set when status → PAID |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**Indexes**:

- `UNIQUE (booking_id, type)` — one deposit + one remaining per booking
- `(customer_id, status, created_at DESC)` — SCR-26 customer history
- `(property_id, status, due_date)` — SCR-36 manager pending filter
- `(booking_id)` — booking detail breakdown

### InvoiceType enum (v1)

`DEPOSIT` | `REMAINING_BALANCE`

Future (FR-12/FR-23): `DAMAGE_FEE` — not issued by FR-11 v1.

### InvoiceStatus enum

`UNPAID` | `PENDING_PAYMENT` | `PAID` | `CANCELLED`

### Status transitions

```text
(issue on booking create)     → UNPAID
Payment attempt started       → PENDING_PAYMENT
Payment confirmed (FR-12)     → PAID (+ paid_at)
Payment failed / abandoned    → UNPAID (from PENDING_PAYMENT)
Booking cancelled / timeout   → CANCELLED (if UNPAID or PENDING_PAYMENT)
```

**Immutability**: `amount` on DEPOSIT invoice MUST NOT change after PAID. REMAINING invoice amount MAY update on booking modify per rules in research.md §4.

### Derived: overdue (not stored)

```text
isOverdue = status NOT IN ('PAID','CANCELLED') AND due_date < CURRENT_DATE
```

## Alter: payments (FR-12 migration V027)

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| invoice_id | UUID | FK invoices nullable → NOT NULL for new rows | FR-12 sets on payment create |

Existing FR-04 deposit payment rows (if any) backfilled by matching `booking_id + type`.

## API DTOs

### InvoiceSummaryResponse

```json
{
  "id": "uuid",
  "bookingId": "uuid",
  "type": "DEPOSIT",
  "amount": 400000,
  "status": "UNPAID",
  "dueDate": "2026-07-01",
  "paidAt": null,
  "isOverdue": false,
  "createdAt": "2026-06-27T10:00:00Z"
}
```

### InvoiceBreakdownResponse (embedded in booking detail)

```json
{
  "invoices": [
    { "id": "...", "type": "DEPOSIT", "amount": 400000, "status": "PAID", "dueDate": "...", "paidAt": "...", "isOverdue": false },
    { "id": "...", "type": "REMAINING_BALANCE", "amount": 600000, "status": "UNPAID", "dueDate": "2026-07-15", "paidAt": null, "isOverdue": false }
  ],
  "totalAmount": 1000000,
  "paidAmount": 400000,
  "remainingBalance": 600000
}
```

### BookingDetailResponse extension

Add `invoiceBreakdown: InvoiceBreakdownResponse` to existing SCR-18 / SCR-35 booking detail payloads.

## Flyway

```text
V026__invoices.sql
V027__payments_invoice_id.sql   # shared with FR-12; FR-11 defines invoice_id column
```

**Note**: Run after FR-04 `bookings` table exists.

## ActivityLog Events

| Event | When |
|-------|------|
| INVOICE_ISSUED | Deposit + remaining pair created |
| INVOICE_PAID | Status → PAID |
| INVOICE_CANCELLED | Booking cancel/timeout |
| INVOICE_ADJUSTED | Remaining amount updated on booking modify |

## Integration Points

| Feature | Integration |
|---------|-------------|
| FR-04 | `createBooking` → issue invoice pair; cancel/timeout → cancel invoices; modify → adjust remaining |
| FR-12 | Payment create/update → `InvoiceStatusSyncService`; Payment MUST set `invoice_id` |
| FR-01 | Customer scope on `/invoices/me` |
| FR-06 | Manager property scope on `/manager/invoices` |
| FR-10 | No direct dependency (contract on deposit paid, not invoice) |
| FR-23 | Future DAMAGE_FEE invoice — out of FR-11 v1 |

## Security

| Endpoint | CUSTOMER | MANAGER | ADMIN |
|----------|----------|---------|-------|
| GET /invoices/me | Own | — | R (global read, no UI v1) |
| GET /bookings/me/{id} invoices | Own booking | — | — |
| GET /manager/invoices | — | Assigned property | R |
| GET /manager/bookings/{id} invoices | — | Assigned property | R |

## Service Layer

| Service | Responsibility |
|---------|----------------|
| `InvoiceIssuanceService` | issuePair, idempotent create |
| `InvoiceAdjustmentService` | modify booking → update remaining |
| `InvoiceCancellationService` | cancel unpaid on booking cancel |
| `InvoiceStatusSyncService` | FR-12 payment hooks |
| `InvoiceQueryService` | list/filter for customer & manager |
