# Research: FR-12 Payment Management & Reconciliation

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Docs**: `docs/Specification_v2.md` FR-12, FR-04 payment stubs, FR-11 invoice sync, `paymentApi.ts`

## 1. Table Ownership

**Decision**: FR-12 **owns** full implementation of `payments` and `payment_receipts` (defined in FR-04 data-model). FR-11 V027 adds `invoice_id` FK — FR-12 sets on every new Payment row.

**Rationale**: Spec FR-003, FR-016; clear split Invoice (FR-11) vs Payment transaction (FR-12).

**Alternatives considered**: Merge Payment into Invoice — rejected (multiple attempts per invoice, VNPay retries).

## 2. Payment Creation Flow

**Decision**: Customer initiates pay → resolve target **Invoice** (DEPOSIT or REMAINING_BALANCE by type) → validate UNPAID → create Payment PENDING linked `invoice_id` → route by method:

| Method | Next step |
|--------|-----------|
| VNPAY | Return redirect URL; invoice → PENDING_PAYMENT via InvoiceStatusSyncService |
| BANK_TRANSFER | Require receipt upload; Payment PENDING; notify Manager |

**Rationale**: Spec US1–US2; one active Pending payment per invoice (reject duplicate).

**Alternatives considered**: Create Payment at booking create — rejected per FR-11 plan (invoice only at create).

## 3. VNPay Integration Pattern

**Decision**: Use official VNPay **v2** redirect + **IPN callback** (server-to-server) + **Return URL** (browser redirect for UX only).

- `VNPayService.createPaymentUrl(payment)` — HMAC-SHA512 hash with `VNPAY_HASH_SECRET`
- `POST /api/v1/payments/vnpay/ipn` — **public**, verify signature, idempotent by `vnp_TxnRef` = `orderRef`
- `GET /api/v1/payments/vnpay/return` — customer landing (success/fail message)
- Env: `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_PAY_URL`, `VNPAY_API_URL` (query), `VNPAY_RETURN_URL`, `VNPAY_IPN_URL`

**Rationale**: Specification_v2 §8 VNPay acceptance; standard VNPay sandbox pattern.

**Alternatives considered**: Client-side only return URL confirm — rejected (unreliable, no IPN).

## 4. orderRef Idempotency

**Decision**: `orderRef = UUID` stored UNIQUE on `payments.order_ref`; sent as `vnp_TxnRef` (max 100 chars). IPN/cron lookup by orderRef before state change.

**Rationale**: Spec FR-004, SC-007.

**Alternatives considered**: Composite bookingId-type — rejected (retry needs new ref per attempt).

## 5. Deposit Confirmation Side-Effects

**Decision**: `PaymentConfirmationService.confirmPaid(payment)` single entry point:

1. Payment → PAID, paidAt
2. `InvoiceStatusSyncService.markPaid(invoiceId)`
3. If type DEPOSIT → `DepositConfirmationService.confirmDeposit(bookingId)` (FR-04): booking CONFIRMED + Outbox CONTRACT_GENERATE_REQUESTED
4. If type REMAINING_BALANCE → update booking.paid_amount only
5. ActivityLog PAYMENT_PAID

**Rationale**: Idempotent side-effects; FR-10/FR-04 triggered once.

**Alternatives considered**: Scatter logic in IPN controller — rejected.

## 6. Manager Verification

**Decision**: `PATCH /manager/payments/{id}/verify` body `{ status: APPROVED|REJECTED, note }`. APPROVED requires `PaymentReceipt` exists for BANK_TRANSFER/CASH. Calls same `confirmPaid` or `markFailed`.

**Rationale**: api-spec SCR-37; RBAC Segregation of Duties §6.

**Alternatives considered**: Separate approve endpoint — align with existing frontend `verifyPayment` POST — migrate to PATCH v1 per api-spec.

## 7. Reconciliation Cron

**Decision**: `@Scheduled(cron = "0 */15 * * * *")` (every 15 min) `PaymentReconciliationJob`:

- Select VNPAY + PENDING where `created_at < now() - 5 minutes`
- Call VNPay **querydr** API with orderRef
- Match response → confirmPaid or markFailed
- Amount mismatch → set `reconciliation_status = DISCREPANCY`

**Rationale**: Spec FR-010, SC-002.

**Alternatives considered**: 5-minute cron — spec says 15 minutes.

## 8. Admin SCR-52 Discrepancy

**Decision**: Add column `payments.reconciliation_status` enum: `NONE`, `DISCREPANCY`, `RESOLVED`. Admin `GET /admin/payments/reconciliation?status=DISCREPANCY` + `POST /admin/payments/{id}/sync-vnpay` triggers same query as cron with audit log PAYMENT_RECONCILED.

**Rationale**: screendesign SCR-52; lightweight without separate table v1.

**Alternatives considered**: Separate reconciliation_audit table — defer to v2 if volume high.

## 9. 24h Manager Reminder

**Decision**: Daily job (or hourly) find BANK_TRANSFER PENDING where `created_at < now() - 24h` AND `reminder_sent_at IS NULL OR reminder_sent_at < now() - 24h`; enqueue FR-15 notification to property Manager; set `reminder_sent_at`.

**Rationale**: Spec FR-013, SC-008.

**Alternatives considered**: Real-time only — insufficient per spec.

## 10. Receipt Upload

**Decision**: `POST /bookings/{id}/receipts` multipart or JSON with pre-uploaded file URL (match FR-08 room upload pattern). Creates/links PaymentReceipt to active Pending bank payment for invoice. Storage `app.upload.receipts-dir`.

**Rationale**: SCR-20 api-spec; existing frontend receipt flow.

**Alternatives considered**: Base64 inline — rejected (size limits).

## 11. property_id Denormalization

**Decision**: Denormalize `property_id` on `payments` from booking→room→property for manager list index `(property_id, status, created_at DESC)`.

**Rationale**: SCR-36 filter performance; FR-06 scope pattern.

## 12. API Path Migration

**Decision**: Standardize under `/api/v1`:

| Legacy (frontend) | Target |
|-----------------|--------|
| `/api/payments/vnpay/create-url` | `POST /api/v1/payments/vnpay/create-url` |
| `/api/manager/payments` | `GET /api/v1/manager/payments` |
| `/api/manager/payments/{id}/verify` POST | `PATCH /api/v1/manager/payments/{id}/verify` |
| — | `GET /api/v1/payments/me` |
| — | `GET /api/v1/admin/payments/reconciliation` |

**Rationale**: api-spec-by-screen.md; project envelope standard.

## 13. Damage Fee Payment (P2)

**Decision**: Stub `DamagePaymentListener.onDamageApproved(event)` creating invoice DAMAGE_FEE (FR-11 extension) + expose pay endpoint when FR-23 ships. v1: interface + no-op or feature flag.

**Rationale**: Spec US6 P2.

## 14. Security

**Decision**: VNPay IPN/return endpoints permitAll with signature verification only; all other payment endpoints JWT + RBAC. Never log hash secret or full card data.

**Rationale**: AGENTS.md security rules.

## 15. Frontend Gap

**Decision**: Migrate `paymentApi.ts` to v1; wire `PaymentPages.tsx`, `PaymentMgmt*`, create `AdminReconciliationPage.tsx` for SCR-52 (new route under admin layout).

**Rationale**: Pages exist except Admin reconciliation — new page required.
