# Data Model: FR-09 Customer Management

**Date**: 2026-07-23  
**Spec**: [spec.md](./spec.md) | Clarifications Session 2026-07-23

## Scope

FR-09 **does not own** new entities. Operates on **User** (`role=CUSTOMER`); reads **Booking** + **Payment**; writes **ActivityLog**; updates **RefreshToken**.

## ERD

```text
User (CUSTOMER) 1──* Booking
User (CUSTOMER) 1──* Payment (via booking / customer link)
User (CUSTOMER) 1──* RefreshToken
Admin ──writes──> User.status (ACTIVE | SUSPENDED)
Admin ──writes──> ActivityLog (USER_SUSPENDED | USER_ACTIVATED)
On SUSPEND ─────> RefreshToken.revoked_at (all active for user)
```

## Entity: User (Customer scope — FR-01 owned)

| Attribute | Type | FR-09 usage |
|-----------|------|-------------|
| id | UUID | PK |
| fullName | String | List, detail, search |
| email | String | List, detail, search |
| phone | String | Detail read-only |
| avatarUrl | String | Detail read-only |
| role | Enum | MUST be CUSTOMER |
| status | Enum | INACTIVE \| ACTIVE \| SUSPENDED |
| createdAt | DateTime | Registered |
| updatedAt | DateTime | Audit |

### Status transitions (FR-09)

```text
ACTIVE    ──Admin Suspend──> SUSPENDED   (+ revoke refresh tokens + ActivityLog USER_SUSPENDED)
SUSPENDED ──Admin Activate─> ACTIVE      (+ ActivityLog USER_ACTIVATED)
INACTIVE  ──(no FR-09 write)──> FR-01 OTP verify → ACTIVE
```

**Validation**:

- Target `role != CUSTOMER` → 403/business reject
- Current status `INACTIVE` → **409**
- Request status must be `ACTIVE` or `SUSPENDED` only (not INACTIVE)

## View: CustomerSummary (list row)

| Field | Source | Notes |
|-------|--------|-------|
| id | users.id | |
| fullName | users.full_name | |
| email | users.email | |
| phone | users.phone | optional |
| status | users.status | Badge |
| avatarUrl | users.avatar_url | optional |
| totalBookings | aggregate | `COUNT(bookings)` by customer_id |
| totalSpend | aggregate | `SUM(payments.amount)` where status=PAID |
| createdAt | users.created_at | |

## View: CustomerDetail (drawer)

Extends CustomerSummary with optional read-only `outstandingDebt` if column exists (damage flow — FR-09 does not write). Nested bookings via separate endpoint.

## Entity: Booking (read-only — FR-04)

| Field in response | Source |
|-------------------|--------|
| id | bookings.id |
| checkInDate | check_in_date |
| checkOutDate | check_out_date |
| totalAmount | total_amount |
| status | status |
| roomNumber | rooms.room_number |
| propertyName | properties.name |

**Sort**: `created_at DESC`

## Aggregate SQL (authoritative)

```sql
-- totalBookings
SELECT COUNT(*) FROM bookings WHERE customer_id = :userId;

-- totalSpend (Clarifications 2026-07-23)
SELECT COALESCE(SUM(p.amount), 0)
FROM payments p
JOIN bookings b ON b.id = p.booking_id   -- or customer_id on payments if present
WHERE /* customer scope */ = :userId
  AND p.status = 'PAID';
```

Align with existing `PaymentRepository.sumPaidAmountByCustomerId` / `sumPaidAmountByCustomerIds`.

## RefreshToken side-effect

On status → `SUSPENDED`:

```sql
UPDATE refresh_tokens
SET revoked_at = CURRENT_TIMESTAMP
WHERE user_id = :userId AND revoked_at IS NULL;
```

Mapped by `RefreshTokenRepository.revokeAllByUser`.

## ActivityLog Events

| Action | When |
|--------|------|
| USER_SUSPENDED | ACTIVE → SUSPENDED success |
| USER_ACTIVATED | SUSPENDED → ACTIVE success |

Payload/details: `{ previousStatus, newStatus }` (or equivalent string); actor = Admin user id.

## Optional indexes

```text
idx_users_role_status ON users (role, status)
idx_bookings_customer_id ON bookings (customer_id)
-- payments: ensure customer/booking FK indexes for SUM PAID
```

Skip if already present from FR-01/04/12.

## Integration Points

| Feature | Integration |
|---------|-------------|
| FR-01 | User entity; login/refresh block SUSPENDED; revoke helper |
| FR-04 | Booking history + count |
| FR-12 | Payment PAID for Total Spend |
| FR-06 | Shared AdminUser directory — **do not** change Manager ACTIVE/INACTIVE rules incorrectly |
| FR-02 | Customer self-profile write — Admin read-only here |
| FR-17 | Complaints — not FR-09 |

## Security matrix

| Endpoint | ADMIN | Others |
|----------|-------|--------|
| GET users?role=CUSTOMER | ✓ | 403 |
| GET users/{id} (CUSTOMER) | ✓ | 403 |
| PATCH/PUT status ACTIVE\|SUSPENDED | ✓ | 403 |
| GET users/{id}/bookings | ✓ | 403 |
