# Research: FR-09 Customer Management

**Date**: 2026-07-23  
**Spec**: [spec.md](./spec.md) | Clarifications Session 2026-07-23  
**Code baseline**: `AdminUserService`, `PaymentRepository.sumPaidAmount*`, `RefreshTokenRepository.revokeAllByUser`, `ActivityLog` action comments

## 1. Table Ownership

**Decision**: FR-09 **does not** create new tables. Writes `users.status`; reads `bookings` + `payments`; writes `activity_logs`; updates `refresh_tokens`.

**Rationale**: User/Booking/Payment/RefreshToken owned by FR-01/04/12; FR-09 is Admin operational layer.

**Alternatives considered**: Separate `customers` table — rejected (duplicate User).

## 2. Admin API Surface

**Decision**: Customer ops on shared Admin User Directory:

| Method | Path (contract target) | Purpose |
|--------|------------------------|---------|
| GET | `/api/v1/admin/users?role=CUSTOMER` | SCR-51 list + filters + aggregates |
| GET | `/api/v1/admin/users/{id}` | Profile detail |
| PATCH | `/api/v1/admin/users/{id}/status` | ACTIVE ↔ SUSPENDED (preferred) |
| GET | `/api/v1/admin/users/{id}/bookings` | Paginated booking history |

**As-is code**: `/api/admin/users` + PUT update status. Implementation may keep PUT with hardened semantics or add PATCH and alias `/api/v1`.

**Rationale**: Spec + SCR-51; Clarifications do not require new resource names.

**Alternatives considered**: Dedicated `/admin/customers` — rejected (existing SCR-50/51 shared directory).

## 3. Status Transition Rules

**Decision**: For `role=CUSTOMER`, Admin may set only `ACTIVE` or `SUSPENDED` when current status is `ACTIVE` or `SUSPENDED`. Current `INACTIVE` → **409** (no Suspend/Activate). FE disables actions for INACTIVE.

**Rationale**: Spec FR-003/Edge Cases; INACTIVE owned by FR-01 OTP.

**Alternatives considered**: Admin force-activate INACTIVE — rejected (bypasses email verification).

## 4. Suspend Session Handling

**Decision**: On transition to `SUSPENDED`, call `RefreshTokenRepository.revokeAllByUser(user)` in the same transaction as status update. FR-01 continues to reject login/refresh for SUSPENDED. Access JWT expires by TTL (no access blacklist v1).

**Rationale**: Clarifications 2026-07-23 (FR-004 / SC-002); helper already used in logout/password flows.

**Alternatives considered**: Natural session expiry only — **rejected** by clarify; access-token blacklist — deferred.

## 5. Total Bookings & Total Spend

**Decision**:

- `totalBookings` = `COUNT(*)` bookings WHERE `customer_id = user.id`
- `totalSpend` = `SUM(Payment.amount)` WHERE customer’s payments AND `status = PAID` (all payment types)

**Rationale**: Clarifications 2026-07-23; already implemented in `PaymentRepository.sumPaidAmountByCustomerId(s)`.

**Alternatives considered**: SUM booking `total_amount` for confirmed+ statuses — superseded by clarify.

## 6. Search & Filter Defaults

**Decision**: Params `search` (name OR email), optional `status` (INACTIVE|ACTIVE|SUSPENDED), `page`, `size`, sort `createdAt,desc`. Hard-filter `role=CUSTOMER`. **Default** (no status param) = **all** statuses including INACTIVE.

**Rationale**: Clarifications 2026-07-23; FR-001.

**Alternatives considered**: Default ACTIVE-only — rejected.

## 7. Booking History Response

**Decision**: id, checkInDate, checkOutDate, roomNumber, propertyName, totalAmount, status — sorted newest first; paginated.

**Rationale**: FR-006; `listCustomerBookings` already exists — FE must wire drawer.

**Alternatives considered**: Embed full booking detail — out of scope.

## 8. Controller Strategy

**Decision**: Keep `AdminUserController` / `AdminUserService`; branch Customer vs Manager status rules so FR-06 Manager ACTIVE/INACTIVE is not broken.

**Rationale**: Code already shared; YAGNI for parallel AdminCustomer* types unless tasks force split.

**Alternatives considered**: New `AdminCustomerController` only — optional refactor, not required for FR-09 DoD.

## 9. Frontend UX

**Decision**: `CustomerDirectoryPage` + `_UserDirectoryShared`: Suspend/Activate with **ConfirmationDialog**; badge for SUSPENDED; filter option SUSPENDED; **Total Spend** column; drawer loads bookings endpoint; INACTIVE → actions disabled.

**Rationale**: Spec FR-003/US stories; screendesign drawer pattern.

**Alternatives considered**: Dedicated detail route — rejected per Assumptions.

## 10. Audit Events

**Decision**: `ActivityLogService.log(...)` with action `USER_SUSPENDED` or `USER_ACTIVATED`; actor = Admin; entityType `User`; entityId = customer id; details include old→new status.

**Rationale**: Clarifications 2026-07-23 (FR-009); constants already documented on `ActivityLog` entity.

**Alternatives considered**: Single `USER_STATUS_CHANGED` — superseded by clarify.

## 11. RBAC & Manager Scope

**Decision**: All FR-09 ops ADMIN-only (403 otherwise). Manager Customer List (figma SCR-55/56) **permanently out of FR-09**.

**Rationale**: Clarifications 2026-07-23.

**Alternatives considered**: Manager read-only list in FR-09 — rejected forever for this feature.

## 12. Indexes (optional)

**Decision**: Prefer existing indexes on `users(role,status)`, `bookings(customer_id)`, `payments(customer/booking)`; add only if list/aggregate p95 fails SC.

**Rationale**: LESSON-008; avoid duplicate Flyway.

**Alternatives considered**: Mandatory new Flyway — not required for MVP close-out.

## Resolved NEEDS CLARIFICATION

All prior unknowns closed in Clarifications Session 2026-07-23 (see `spec.md` Clarifications + `CONTEXT.md` OPEN QUESTIONS). No remaining research blockers.
