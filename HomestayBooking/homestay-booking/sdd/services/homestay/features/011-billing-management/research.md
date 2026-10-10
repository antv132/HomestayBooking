# Research: FR-11 Billing Management

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Docs**: `docs/Specification_v2.md` FR-11, §5 Booking/Payment, FR-04 data-model, `paymentApi.ts`, `BookingPages.tsx`

## 1. Invoice vs Payment Separation

**Decision**: Introduce dedicated `invoices` table (billing obligation). `payments` (FR-12) MUST reference `invoice_id` when a payment attempt is created. FR-11 **does not** create Payment rows.

**Rationale**: Spec FR-001–FR-012 and Assumptions explicitly split billing document (Invoice) from payment execution (Payment). Aligns with FR-11/FR-12 boundary in Specification_v2.

**Alternatives considered**: Reuse `payments` row as invoice — rejected (mixes obligation with transaction attempts; multiple VNPay retries need multiple payments per invoice).

## 2. FR-04 Integration Adjustment

**Decision**: On `POST /bookings`, FR-04 `BookingService.createBooking()` calls `InvoiceIssuanceService.issuePair(booking)` in same `@Transactional` block. **Remove** FR-04 side-effect "insert Payment DEPOSIT PENDING" — defer Payment creation to FR-12 when customer initiates pay.

**Rationale**: Avoid duplicate deposit tracking; invoice exists before any payment attempt.

**Alternatives considered**: Keep FR-04 Payment insert + add invoices — rejected (two sources of truth for deposit amount).

## 3. Idempotency

**Decision**: `UNIQUE (booking_id, type)` on `invoices` where `type IN ('DEPOSIT', 'REMAINING_BALANCE')`. Service checks before insert; safe on booking-create retry.

**Rationale**: Spec FR-002, SC-007.

**Alternatives considered**: Application-only guard — rejected under concurrency.

## 4. Amount Snapshot Rules

**Decision**:

| Event | Deposit invoice | Remaining invoice |
|-------|-----------------|-------------------|
| Booking create | `deposit_amount` from booking | `remaining_amount` from booking |
| Booking modify (pre-deposit paid) | Update both if Unpaid | Update both if Unpaid |
| Booking modify (deposit Paid) | Immutable | Update to new `remaining_amount` |
| Booking cancel | Cancel if Unpaid/Pending | Cancel if Unpaid/Pending |

**Rationale**: Spec FR-003, FR-004, US-4.

**Alternatives considered**: New adjustment invoice rows for delta — YAGNI v1; update remaining amount in place with ActivityLog `INVOICE_ADJUSTED`.

## 5. Invoice Status Enum

**Decision**: DB enum `UNPAID`, `PENDING_PAYMENT`, `PAID`, `CANCELLED`. **Overdue** computed in DTO: `status NOT IN (PAID, CANCELLED) AND due_date < CURRENT_DATE`.

**Rationale**: Spec FR-005; Overdue is display-only v1.

**Alternatives considered**: Persist OVERDUE status — rejected (requires cron to flip rows).

## 6. Due Dates

**Decision**: Deposit invoice `due_date = booking.created_at::date` (due immediately). Remaining invoice `due_date = booking.check_in_date`.

**Rationale**: Spec Assumptions.

**Alternatives considered**: Remaining due at checkout — rejected (CHECKIN_DENIED_UNPAID requires pre-check-in payment).

## 7. Status Sync from Payment (FR-12 Hook)

**Decision**: `InvoiceStatusSyncService` interface consumed by FR-12:

| Payment event | Invoice transition |
|---------------|-------------------|
| Payment created (PENDING) | UNPAID → PENDING_PAYMENT |
| Payment PAID | → PAID, set `paid_at` |
| Payment FAILED (last attempt) | PENDING_PAYMENT → UNPAID |
| Payment REFUNDED | PAID stays; refund tracked on Payment (FR-12) |

**Rationale**: Spec FR-006; FR-11 owns invoice state; FR-12 calls sync on payment lifecycle.

**Alternatives considered**: DB trigger — rejected (business logic in service layer per constitution).

## 8. Booking Cancel / Timeout Hook

**Decision**: `InvoiceCancellationService.cancelUnpaidForBooking(bookingId)` invoked from FR-04 cancel/timeout paths in same transaction.

**Rationale**: Spec FR-007, SC-006.

**Alternatives considered**: Async listener — acceptable but same-transaction simpler for consistency.

## 9. API Surface

**Decision**:

| Endpoint | Role | Screen |
|----------|------|--------|
| `GET /invoices/me` | CUSTOMER | SCR-26 |
| `GET /bookings/me/{id}` extended `invoices[]` | CUSTOMER | SCR-18 |
| `GET /manager/bookings/{id}` extended `invoices[]` | MANAGER | SCR-35 |
| `GET /manager/invoices?propertyId=&status=` | MANAGER | SCR-36 (pending/unpaid filter) |

**Rationale**: api-spec-by-screen; embed breakdown in booking detail per screendesign Payment Breakdown; dedicated list for manager filter.

**Alternatives considered**: Invoice-only API without booking embed — rejected (SCR-18 requires inline breakdown).

## 10. Manager Property Scope

**Decision**: Filter via `booking.room.property_id IN managerAssignedPropertyIds`; denormalize `property_id` on invoice row for index performance.

**Rationale**: FR-06 PropertyAccessValidator pattern; spec FR-009.

**Alternatives considered**: Join-only filter — acceptable v1; denormalize preferred for SCR-36 list speed.

## 11. Migration Order

**Decision**:

```text
V026__invoices.sql          # FR-11 owns
V027__payments_invoice_id.sql  # nullable FK; FR-12 backfills + requires for new payments
```

Run after FR-04 bookings (V0xx) and before FR-12 full payment implementation.

**Rationale**: Invoice must exist before payment references it.

**Alternatives considered**: Single migration with payments rewrite — too coupled with FR-12 scope.

## 12. Frontend Alignment

**Decision**: Extend `BookingDetailResponse.invoices[]`; update `BookingPages.tsx` / `BookingDetailPage.tsx` Payment Breakdown to read invoices (fallback to legacy `payments[]` during migration). `PaymentHistoryPage` calls `GET /invoices/me`.

**Rationale**: Frontend already has payment breakdown UI using `payments` — minimal change to swap data source.

**Alternatives considered**: New InvoiceListPage — rejected (no dedicated SCR).

## 13. paid_amount Denormalization on Booking

**Decision**: Keep FR-04 `bookings.paid_amount` cache; update via `InvoiceStatusSyncService` when invoice → PAID (sum of paid invoice amounts).

**Rationale**: SCR-18 `paidAmount` field; avoids recalc on every read.

**Alternatives considered**: Computed on read only — acceptable but slower for list views.

## 14. Damage Fee Invoices

**Decision**: **Out of scope v1**. FR-12/FR-23 will create `DAMAGE_FEE` invoice type in future migration when damage flow ships.

**Rationale**: Spec scope boundary.

**Alternatives considered**: Enum placeholder in DB — add `type` values now but no handler until FR-23.

## 15. ActivityLog Events

**Decision**: Log `INVOICE_ISSUED` (pair create), `INVOICE_PAID`, `INVOICE_CANCELLED`, `INVOICE_ADJUSTED` with `{ bookingId, invoiceId, type, amount }`.

**Rationale**: Spec FR-011; AGENTS.md audit requirements.

**Alternatives considered**: Skip ISSUED for remaining — log both on pair create as single event with payload array.
