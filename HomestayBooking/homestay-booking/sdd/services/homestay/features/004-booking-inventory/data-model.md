# Data Model: FR-04 Booking & Inventory Management

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: `docs/Specification_v2.md` §5 Booking, Payment, PaymentReceipt, OutboxEvent, HousekeepingTask, RoomInspection

## Scope

FR-04 **owns** Booking lifecycle, inventory locks, booking-triggered outbox events, and integration stubs for Contract (FR-10), Payment reconciliation (FR-12), Inspection (FR-23), Housekeeping (FR-21).

## ERD

```text
Customer 1──* Booking *──1 Room *──1 Property
Booking 1──* Payment 1──* PaymentReceipt
Booking 1──1 BookingInventoryLock
Booking 1──0..1 RoomInspection
Booking 1──0..* OutboxEvent (via bookingId in payload)
Room 1──* HousekeepingTask (created on checkout)
```

## Tables

### bookings

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| customer_id | UUID | FK users, NOT NULL | |
| room_id | UUID | FK rooms, NOT NULL | |
| check_in_date | DATE | NOT NULL | |
| check_out_date | DATE | NOT NULL, > check_in | |
| guest_count | INT | NOT NULL, > 0 | |
| special_requests | TEXT | nullable | |
| total_amount | DECIMAL(15,2) | NOT NULL | Snapshot |
| deposit_amount | DECIMAL(15,2) | NOT NULL | 40% at create |
| remaining_amount | DECIMAL(15,2) | NOT NULL | 60% at create |
| damage_fee_amount | DECIMAL(15,2) | DEFAULT 0 | From FR-23 |
| paid_amount | DECIMAL(15,2) | DEFAULT 0 | Denormalized cache |
| status | VARCHAR(32) | NOT NULL | Enum below |
| payment_method | VARCHAR(16) | NOT NULL | VNPAY, BANK_TRANSFER |
| hold_expires_at | TIMESTAMPTZ | nullable | Set on PENDING_DEPOSIT |
| cancelled_by | UUID | FK users, nullable | |
| cancel_reason | TEXT | nullable | |
| cancelled_at | TIMESTAMPTZ | nullable | |
| row_version | INT | NOT NULL DEFAULT 0 | Optimistic lock |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**Indexes**: `(customer_id, status)`, `(room_id, check_in_date, check_out_date)`, `(status, hold_expires_at)` for timeout job, `(status, check_in_date)` for no-show job.

#### BookingStatus enum

| Value | Inventory locked? | Terminal? |
|-------|-------------------|-----------|
| PENDING_DEPOSIT | Yes | No |
| CONFIRMED | Yes | No |
| CHECKED_IN | Yes | No |
| PENDING_INSPECTION | Yes | No |
| PENDING_DAMAGE_PAYMENT | Yes | No |
| CHECKED_OUT | No (released) | Yes |
| CANCELLED | No (released) | Yes |
| NO_SHOW | No (released) | Yes |

**Blocking statuses for FR-03 availability** (overlap queries): all except `CANCELLED`, `CHECKED_OUT`, `NO_SHOW`.

### Inventory overlap (SQL Server)

Không dùng bảng `booking_inventory_locks` / `EXCLUDE USING gist` (PostgreSQL-only).

Trong transaction tạo booking:

1. `RoomRepository.findByIdForUpdate(roomId)` — pessimistic write lock trên hàng `rooms`
2. `RoomRepository.existsOverlapBooking(roomId, checkIn, checkOut)` — half-open overlap booking chưa terminal
3. `RoomStatusBlockRepository.existsActiveOverlappingStay(roomId, checkIn, checkOut)` — block MAINTENANCE/OOS
4. Nếu overlap/block → `409 Conflict`; ngược lại insert `bookings`

**Blocking statuses**: tất cả trừ `CANCELLED`, `CHECKED_OUT`, `NO_SHOW`.

Khoảng ngày inventory: **half-open** `[check_in, check_out)` (Spec Sync clarify 2026-07-23) — overlap khi `check_in < other_check_out AND check_out > other_check_in`. Same-day turnover được phép. Create/modify cũng MUST kiểm tra overlap với `RoomStatusBlock` active (MAINTENANCE/OOS).

### payments (FR-04 creates deposit rows; FR-12 extends)

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| booking_id | UUID | FK |
| customer_id | UUID | FK |
| type | ENUM | DEPOSIT, REMAINING_BALANCE, DAMAGE_FEE, REFUND |
| amount | DECIMAL(15,2) | |
| method | ENUM | VNPAY, BANK_TRANSFER, CASH |
| status | ENUM | PENDING, PAID, FAILED, REFUNDED |
| order_ref | VARCHAR(64) | UNIQUE idempotency |
| gateway_transaction_id | VARCHAR | VNPay |
| verified_by | UUID | Manager for bank transfer |
| verified_at | TIMESTAMPTZ | |
| verification_note | TEXT | |
| paid_at | TIMESTAMPTZ | |
| created_at, updated_at | TIMESTAMPTZ | |

### payment_receipts

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| payment_id | UUID | FK payments |
| file_url | VARCHAR(512) | |
| file_name | VARCHAR | |
| file_size | BIGINT | |
| created_at | TIMESTAMPTZ | |

### outbox_events (write on CONFIRMED)

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| event_type | VARCHAR | e.g. CONTRACT_GENERATE_REQUESTED |
| payload | JSONB | `{ "bookingId": "..." }` |
| status | ENUM | PENDING, PROCESSED, FAILED |
| retry_count | INT | |
| created_at, processed_at | TIMESTAMPTZ | |

### room_inspections (minimal stub for FR-04 checkout gate)

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| booking_id | UUID | FK UNIQUE |
| room_id | UUID | FK |
| status | ENUM | PENDING, IN_PROGRESS, PASSED, FAILED |
| note | TEXT | |
| inspected_by | UUID | Employee |
| created_at, updated_at | TIMESTAMPTZ | |

FR-23 expands; FR-04 only checks `PASSED` before checkout.

### housekeeping_tasks (insert on CHECKED_OUT)

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| property_id | UUID | FK |
| room_id | UUID | FK |
| booking_id | UUID | FK nullable |
| status | ENUM | PENDING (default on create) |
| created_at | TIMESTAMPTZ | |

FR-21 assigns employee and state transitions.

## State Transition Rules

| From | Event | To | Side effects |
|------|-------|-----|--------------|
| — | createBooking | PENDING_DEPOSIT | Insert lock; hold_expires_at = now + timeout; Payment DEPOSIT PENDING |
| PENDING_DEPOSIT | depositPaid | CONFIRMED | Outbox CONTRACT_GENERATE_REQUESTED |
| PENDING_DEPOSIT | holdTimeout | CANCELLED | Delete lock |
| PENDING_DEPOSIT | customerCancel | CANCELLED | Delete lock |
| CONFIRMED | customerCancel | CANCELLED | Refund tier; delete lock |
| CONFIRMED | managerCancel | CANCELLED | 100% refund; delete lock |
| CONFIRMED | noShowJob | NO_SHOW | Deposit kept; delete lock |
| CONFIRMED | checkIn | CHECKED_IN | Room → OCCUPIED |
| CHECKED_IN | requestCheckout | PENDING_INSPECTION | Create inspection if absent |
| PENDING_INSPECTION | inspectionPassed + paid | CHECKED_OUT | Room PENDING_CLEANING; HousekeepingTask |
| PENDING_INSPECTION | damageApproved unpaid | PENDING_DAMAGE_PAYMENT | |
| PENDING_DAMAGE_PAYMENT | damagePaid | CHECKED_OUT | Same checkout side effects |

## Validation Rules

| Rule | Error |
|------|-------|
| check_out > check_in | 400 field error |
| check_in >= today (Asia/Ho_Chi_Minh) | 400 |
| guest_count <= room.capacity | 400 |
| room.status bookable (not MAINTENANCE/OOS/cleaning) | 409 ROOM_NOT_AVAILABLE |
| overlapping active booking (half-open) | 409 ROOM_NOT_AVAILABLE |
| overlapping active RoomStatusBlock (MAINTENANCE/OOS) | 409 ROOM_NOT_AVAILABLE |
| cancel after CHECKED_IN | 409 CANNOT_CANCEL |
| modify non-CONFIRMED | 409 INVALID_STATUS |
| check-out without inspection complete | 409 INSPECTION_REQUIRED |
| check-out with unpaid DAMAGE_FEE | 409 PAYMENT_REQUIRED |

## DTOs (summary)

### CreateBookingRequest

```json
{
  "roomId": "uuid",
  "checkIn": "2026-07-01",
  "checkOut": "2026-07-03",
  "guestCount": 2,
  "specialRequests": "Late check-in",
  "paymentMethod": "VNPAY"
}
```

### CreateBookingResponse

```json
{
  "bookingId": "uuid",
  "status": "PENDING_DEPOSIT",
  "totalAmount": 1000000,
  "depositAmount": 400000,
  "remainingAmount": 600000,
  "holdExpiresAt": "2026-06-27T12:30:00+07:00",
  "paymentUrl": "https://..."
}
```

`paymentUrl` only for VNPay deposit init (edu). Bank transfer create path **out of scope** (TD-011).

### Damage fee (FR-013 clarify)

- `remainingAmount` = 60% stay snapshot only  
- `damageFeeAmount` separate; payment `type=DAMAGE_FEE`  
- Detail “amount due” = unpaid remaining + unpaid damage (computed)

### BookingDetail (customer/manager variants)

Customer: includes `paidAmount`, `remainingAmount`, `damageFeeAmount`, `holdExpiresAt`, actions (PAY_DEPOSIT, CANCEL, PAY_REMAINING, PAY_DAMAGE — **no** UPLOAD_RECEIPT in edu).  
Manager: adds customer contact, payment breakdown, check-in/out blocked reasons.

### CancellationPreview

```json
{
  "daysUntilCheckIn": 5,
  "refundPercent": 50,
  "refundAmount": 200000,
  "forfeitAmount": 200000,
  "policyText": "..."
}
```

## Flyway Migration Order

```text
V0xx__bookings_table.sql
V0xx__payments_booking_fk.sql (if not in FR-12)
V0xx__outbox_events.sql
V0xx__room_inspections_stub.sql
V0xx__housekeeping_tasks_booking_id.sql
```

## Audit Log Actions (ActivityLog)

- `BOOKING_CREATED`, `BOOKING_CONFIRMED`, `BOOKING_CANCELLED`, `BOOKING_CHECKED_IN`, `BOOKING_CHECKED_OUT`, `BOOKING_NO_SHOW`, `BOOKING_MODIFIED`
