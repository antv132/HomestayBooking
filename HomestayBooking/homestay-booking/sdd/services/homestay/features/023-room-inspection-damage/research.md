# Research: FR-23 Room Inspection & Damage Resolution

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Docs**: api-spec SCR-42/43/53/62/63/64, FR-04 data-model stub

## 1. Expand Stub vs New Migration

**Decision**: **V039** `ALTER`/recreate `room_inspections` to full schema + create `damage_reports`, `damage_items`. Migrate FR-04 stub columns forward.

**Rationale**: FR-04 V023 stub lacks `property_id`, `checklist`, `inspected_at`; FR-23 owns full lifecycle.

**Alternatives considered**: Separate `inspection_v2` table — rejected (one inspection per booking constraint).

## 2. Inspection Creation Trigger

**Decision**: `RoomInspectionService.createForBooking(bookingId)` called from FR-04 `BookingService.requestCheckout()` when status CHECKED_IN → PENDING_INSPECTION. Idempotent: unique `booking_id`.

**Rationale**: Spec assumption; aligns FR-04 state machine `requestCheckout → PENDING_INSPECTION`.

## 3. Inspection Status Transitions

**Decision**:

```text
(create) → PENDING
Employee start checklist → IN_PROGRESS (sets inspected_by)
Pass → PASSED (sets inspected_at)
Fail → FAILED_WITH_DAMAGE (sets inspected_at)
PASSED | FAILED_WITH_DAMAGE → terminal v1
```

**Rationale**: Spec US1; api-spec uses PASS/FAIL — map to PASSED / FAILED_WITH_DAMAGE enums.

## 4. Checklist Storage

**Decision**: JSONB column `checklist` on `room_inspections` e.g. `{ "tv": true, "minibar": true, "bed": true, "bathroom": true }`.

**Rationale**: screendesign SCR-62; flexible without child table v1.

## 5. Damage Report Creation

**Decision**: POST creates `damage_reports` + `damage_items` in one transaction; status `PENDING_APPROVAL`; requires linked inspection `FAILED_WITH_DAMAGE`; `total_estimated_cost` = SUM items.

**Rationale**: Spec US2; Employee-only create per RBAC matrix.

## 6. Manager Approval & Escalation

**Decision**:

- `fee <= escalationThreshold` (default **5_000_000** VND): status → `APPROVED`, set `approved_amount`, `approved_by`, `approved_at`; apply damage fee to booking.
- `fee > threshold`: status → `ESCALATED`, `requires_admin_escalation = true`; **no** balance update until Admin co-approve.

**Rationale**: Spec US4/US5; api-spec SCR-43/53.

**Config**: `app.damage.escalation-threshold-vnd=5000000` in `DamageEscalationProperties`.

## 7. Admin Co-Approve

**Decision**: PATCH co-approve only when status `ESCALATED`; sets `admin_approver_id`, status `APPROVED`, then same balance + payment side-effects as Manager approve.

**Rationale**: Segregation of Duties; Admin cannot approve sub-threshold directly.

## 8. Damage Fee Application (FR-12 Integration)

**Decision**: On `APPROVED` (Manager or after Admin co-approve):

1. `booking.damage_fee_amount = approved_amount`
2. `booking.remaining_balance += approved_amount` (or dedicated damage balance field)
3. Emit `PaymentService.createPendingDamageFee(bookingId, amount)` — type `DAMAGE_FEE`
4. Emit FR-15 notification to Customer

**Rationale**: Spec FR-011/FR-012; FR-12 owns payment record.

## 9. Checkout Gate

**Decision**: `InspectionCheckoutGateService.assertCanCheckout(bookingId)`:

| Condition | Allow checkout |
|-----------|----------------|
| No inspection or not terminal | **DENY** `INSPECTION_REQUIRED` |
| PASSED, no damage fee | Allow if remaining balance paid |
| FAILED_WITH_DAMAGE, no report | **DENY** `DAMAGE_REPORT_REQUIRED` |
| Report PENDING_APPROVAL / ESCALATED / DISPUTED | **DENY** |
| Report APPROVED, damage payment pending | **DENY** `DAMAGE_FEE_UNPAID` |
| Report APPROVED, damage paid (or amount 0) | Allow |

Called from FR-04 `completeCheckout` before CHECKED_OUT.

**Rationale**: Spec US3; SC-002.

## 10. Customer Dispute

**Decision**: `PATCH /customer/damage-reports/{id}/dispute` within 24h of `approved_at` (Asia/Ho_Chi_Minh). Status → `DISPUTED`; block damage payment; notify Admin (FR-15). v1: no Admin resolve UI — manual ops P2.

**Rationale**: Spec US6; RBAC Customer R(Dispute).

## 11. Outstanding Debt (P2)

**Decision**: `users.outstanding_debt BOOLEAN DEFAULT false`; Manager `PATCH .../mark-outstanding-debt`; FR-04 `createBooking` checks flag.

**Rationale**: Spec US7 Off-site Collection.

## 12. Attachments

**Decision**: Reuse FR-13 `attachments` table with `entity_type = DAMAGE_ITEM` or `DAMAGE_REPORT`; max 5 images per report align FR-13.

**Rationale**: Avoid duplicate attachment infrastructure.

## 13. Employee List Queries

**Decision**:

- SCR-62 list: bookings CHECKED_IN/PENDING_INSPECTION at employee properties with check-out >= today OR inspection assigned to employee
- SCR-63: damage reports created by employee or at employee properties

**Rationale**: FR-20 property scope + spec US1.

## 14. API Alignment

**Decision**: Extend api-spec minimal payloads:

- Employee: `GET /employee/room-inspections`, `POST /employee/room-inspections`, `PATCH /employee/room-inspections/{id}/submit`
- Customer dispute endpoint (not in api-spec — add to contract)
- Manager: `PATCH /manager/damage-reports/{id}/mark-outstanding-debt` P2

**Rationale**: api-spec POST only for inspection — need list + submit for SCR-62 UX.

## 15. Notifications (FR-15)

**Decision**: Emit outbox/notification events:

- `DAMAGE_REPORT_SUBMITTED` → Manager
- `DAMAGE_REPORT_APPROVED` → Customer
- `DAMAGE_REPORT_ESCALATED` → Admin
- `DAMAGE_REPORT_DISPUTED` → Admin

**Rationale**: Spec FR-017.

## 16. Security

**Decision**: Property scope via `PropertyScopeService` (FR-06/20); Customer dispute scoped by `booking.customer_id`; never accept propertyId from untrusted body for scope bypass.

**Rationale**: Spec FR-016; SC-006.
