# 004 - Booking & Inventory: Database Queries

This document outlines the critical SQL queries used for inventory management and the booking lifecycle.

## 1. Inventory Overlap (Locking mechanism)

*(These queries run inside a database transaction during booking creation or modification)*

### Pessimistic Lock on Room
```sql
SELECT id FROM rooms WHERE id = :roomId FOR UPDATE;
```

### Check Booking Overlap (Half-open intervals)
Ensures no existing active booking overlaps the requested `[checkIn, checkOut)` dates.
```sql
SELECT CASE WHEN EXISTS (
    SELECT 1 FROM bookings 
    WHERE room_id = :roomId 
      AND status NOT IN ('CANCELLED', 'CHECKED_OUT', 'NO_SHOW')
      AND check_in_date < :requestedCheckOut 
      AND check_out_date > :requestedCheckIn
) THEN 1 ELSE 0 END;
```

### Check Maintenance/OOS Overlap
Ensures the room is not under maintenance during the requested dates.
```sql
SELECT CASE WHEN EXISTS (
    SELECT 1 FROM room_status_blocks 
    WHERE room_id = :roomId 
      AND status IN ('MAINTENANCE', 'OUT_OF_SERVICE')
      AND is_active = true
      AND start_date < :requestedCheckOut 
      AND end_date > :requestedCheckIn
) THEN 1 ELSE 0 END;
```

## 2. Booking Creation

### Insert Booking
```sql
INSERT INTO bookings (
    id, customer_id, room_id, check_in_date, check_out_date, guest_count,
    total_amount, deposit_amount, remaining_amount, status, payment_method, 
    hold_expires_at, created_at, updated_at
) VALUES (
    :id, :customerId, :roomId, :checkIn, :checkOut, :guestCount,
    :total, :deposit, :remaining, 'PENDING_DEPOSIT', 'VNPAY',
    CURRENT_TIMESTAMP + INTERVAL '10 minutes', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
```

### Insert Outbox Event (On Payment Success -> CONFIRMED)
```sql
INSERT INTO outbox_events (id, event_type, payload, status, created_at)
VALUES (:id, 'CONTRACT_GENERATE_REQUESTED', :jsonPayload, 'PENDING', CURRENT_TIMESTAMP);
```

## 3. Scheduled Jobs (Cron)

### Find Expired Holds (Hold Timeout Job)
```sql
SELECT id FROM bookings 
WHERE status = 'PENDING_DEPOSIT' 
  AND hold_expires_at < CURRENT_TIMESTAMP;
```

### Find No-Shows (No-Show Job)
`cutoffTime` is calculated in application code (e.g., `checkInDate.atStartOfDay() + 24 hours`).
```sql
SELECT id FROM bookings 
WHERE status = 'CONFIRMED' 
  AND check_in_date < :cutoffDate; -- Based on application date math logic
```

## 4. Manager Check-out Validation

### Check dependencies before check-out
*(Application ensures inspection is PASSED and damage is paid if applicable)*
```sql
-- Fetch Inspection
SELECT status FROM room_inspections WHERE booking_id = :bookingId;

-- Fetch Damage Fee Payment status (if damage_fee_amount > 0)
SELECT status FROM payments 
WHERE booking_id = :bookingId AND type = 'DAMAGE_FEE';
```

### Create Housekeeping Task (Post Check-out)
```sql
INSERT INTO housekeeping_tasks (id, property_id, room_id, booking_id, status, created_at)
VALUES (:id, :propertyId, :roomId, :bookingId, 'PENDING', CURRENT_TIMESTAMP);
```
