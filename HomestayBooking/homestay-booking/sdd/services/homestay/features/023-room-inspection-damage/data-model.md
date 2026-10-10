# Data Model: FR-23 Room Inspection & Damage Resolution

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: `docs/Specification_v2.md` §5 RoomInspection, DamageReport, DamageItem

## Scope

FR-23 **owns** `room_inspections` (expand from FR-04 stub), `damage_reports`, `damage_items`. **Reads/updates** `bookings` (damage fee, checkout gate). **Reuses** `attachments` (FR-13). **Updates** `users.outstanding_debt` (P2). **Integrates** FR-12 `payments` (DAMAGE_FEE).

## ERD

```text
Booking 1──1 RoomInspection
RoomInspection 1──0..1 DamageReport
DamageReport 1──* DamageItem
DamageItem 1──* Attachment (entity_type=DAMAGE_ITEM)
Booking 1──* Payment (DAMAGE_FEE)
User (Customer) outstanding_debt flag P2

RoomInspectionService:
  createForBooking(bookingId)
  listForEmployee(employeeId, propertyIds)
  submitInspection(id, checklist, result, employeeId)

DamageReportService:
  create(report + items + attachments, employeeId)
  listForEmployee / listForManager
  approve(managerId, fee) → APPROVED | ESCALATED
  coApprove(adminId, approvedFee) → APPROVED
  dispute(customerId, note) → DISPUTED
  markOutstandingDebt(managerId) P2

InspectionCheckoutGateService:
  assertCanCheckout(bookingId)
```

## Table: room_inspections (V039 expand)

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| booking_id | UUID | FK bookings UNIQUE NOT NULL | one per booking |
| room_id | UUID | FK rooms NOT NULL | |
| property_id | UUID | FK properties NOT NULL | scope index |
| inspected_by | UUID | FK users nullable | Employee |
| status | VARCHAR(30) | NOT NULL | enum below |
| checklist | JSONB | nullable | TV, minibar, bed, bathroom |
| note | TEXT | nullable | |
| inspected_at | TIMESTAMPTZ | nullable | on Pass/Fail |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**Indexes**:

- `(property_id, status, created_at DESC)` — SCR-42 Manager list
- `(inspected_by, status)` — SCR-62 Employee / FR-22 dashboard
- `(booking_id)` UNIQUE — idempotency

### RoomInspectionStatus

`PENDING` | `IN_PROGRESS` | `PASSED` | `FAILED_WITH_DAMAGE`

```text
create → PENDING
start → IN_PROGRESS (+ inspected_by)
submit PASS → PASSED
submit FAIL → FAILED_WITH_DAMAGE
terminal: PASSED, FAILED_WITH_DAMAGE (v1)
```

## Table: damage_reports

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| inspection_id | UUID | FK room_inspections UNIQUE NOT NULL | |
| booking_id | UUID | FK bookings NOT NULL | denormalized |
| property_id | UUID | FK properties NOT NULL | scope |
| created_by | UUID | FK users NOT NULL | Employee |
| total_estimated_cost | DECIMAL(15,2) | NOT NULL | sum items |
| approved_amount | DECIMAL(15,2) | nullable | Manager/Admin approved |
| approved_by | UUID | FK users nullable | Manager |
| approved_at | TIMESTAMPTZ | nullable | dispute window start |
| requires_admin_escalation | BOOLEAN | DEFAULT false | |
| admin_approver_id | UUID | FK users nullable | |
| admin_approved_at | TIMESTAMPTZ | nullable | |
| status | VARCHAR(30) | NOT NULL | enum below |
| note | TEXT | nullable | Manager note |
| dispute_note | TEXT | nullable | Customer dispute |
| disputed_at | TIMESTAMPTZ | nullable | |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**Indexes**:

- `(property_id, status, created_at DESC)` — SCR-43
- `(booking_id)` — lookup
- `(status)` WHERE status = 'ESCALATED' — SCR-53

### DamageReportStatus

`PENDING_APPROVAL` | `ESCALATED` | `APPROVED` | `DISPUTED` | `PAID`

```text
create → PENDING_APPROVAL
approve fee > threshold → ESCALATED
approve fee <= threshold → APPROVED (+ side effects)
coApprove → APPROVED
customer dispute (24h) → DISPUTED
damage payment completed → PAID
```

## Table: damage_items

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| damage_report_id | UUID | FK damage_reports NOT NULL | |
| item_name | VARCHAR(200) | NOT NULL | |
| description | TEXT | nullable | |
| estimated_cost | DECIMAL(15,2) | NOT NULL | >= 0 |
| created_at | TIMESTAMPTZ | NOT NULL | |

**Index**: `(damage_report_id)`

## Booking updates (FR-04)

| Column | Notes |
|--------|-------|
| damage_fee_amount | Set on APPROVED |
| remaining_balance | += approved_amount |

## users (P2)

| Column | Notes |
|--------|-------|
| outstanding_debt | BOOLEAN DEFAULT false; Manager mark; clear on DAMAGE_FEE paid |

## Checkout Gate Rules

| Inspection | Damage Report | Payment | Checkout |
|------------|---------------|---------|----------|
| missing/PENDING/IN_PROGRESS | — | — | DENY |
| PASSED | none | balance paid | ALLOW |
| FAILED_WITH_DAMAGE | missing | — | DENY |
| FAILED_WITH_DAMAGE | PENDING/ESCALATED/DISPUTED | — | DENY |
| FAILED_WITH_DAMAGE | APPROVED | damage unpaid | DENY |
| FAILED_WITH_DAMAGE | APPROVED/PAID | all paid | ALLOW |

## Error Codes

| Code | HTTP | When |
|------|------|------|
| INSPECTION_REQUIRED | 409 | Checkout without completed inspection |
| DAMAGE_REPORT_REQUIRED | 409 | Failed inspection without report |
| DAMAGE_FEE_UNPAID | 409 | Approved damage not paid |
| UNAUTHORIZED_PROPERTY_ACCESS | 403 | Scope violation |
| DISPUTE_WINDOW_EXPIRED | 400 | Dispute after 24h |
| INSPECTION_ALREADY_TERMINAL | 400 | Re-submit passed/failed |
| ESCALATION_REQUIRED | 409 | Manager approve >5M without Admin |

## Cross-FR Dependencies

| FR | Relationship |
|----|--------------|
| FR-04 | requestCheckout creates inspection; completeCheckout calls gate |
| FR-12 | DAMAGE_FEE payment on approve |
| FR-15 | notification events |
| FR-20/06 | property scope |
| FR-21 | housekeeping after successful checkout |
| FR-22 | read-only inspection list on dashboard |

## Migration: V039__room_inspection_damage_fr23.sql

- Expand `room_inspections` (add property_id, checklist JSONB, align status values)
- CREATE `damage_reports`, `damage_items`
- ALTER `bookings` add `damage_fee_amount` if missing
- ALTER `users` add `outstanding_debt` BOOLEAN DEFAULT false (P2)
- Indexes per above
