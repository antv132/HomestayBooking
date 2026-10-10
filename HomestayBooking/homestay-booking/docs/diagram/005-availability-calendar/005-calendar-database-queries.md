# 005 - Availability Calendar: Database Queries

This document outlines the queries used by the `CalendarStatusService` to fetch overlapping states, and the `RoomStatusBlockService` for validation.

## 1. Calendar Resolution (Read Queries)

### Fetch Overlapping Bookings for the Month
```sql
SELECT id, check_in_date, check_out_date, status
FROM bookings
WHERE room_id = :roomId
  AND status IN ('PENDING_DEPOSIT', 'CONFIRMED', 'CHECKED_IN', 'PENDING_INSPECTION', 'PENDING_DAMAGE_PAYMENT')
  AND check_in_date <= :endOfMonth
  AND check_out_date >= :startOfMonth;
```

### Fetch Active Manual Blocks for the Month
```sql
SELECT id, start_date, end_date, status, reason
FROM room_status_blocks
WHERE room_id = :roomId
  AND active = true
  AND start_date <= :endOfMonth
  AND end_date >= :startOfMonth;
```

### Fetch Open Housekeeping Tasks
```sql
SELECT id, status
FROM housekeeping_tasks
WHERE room_id = :roomId
  AND status NOT IN ('COMPLETED', 'CANCELLED');
```

## 2. Block Validation (Write Queries)

*(Runs during `POST /api/v1/manager/rooms/{id}/blocks`)*

### Check for conflicting Bookings
To prevent a Manager from setting `MAINTENANCE` when a guest is booked. Uses half-open logic `[start, end)`.
```sql
SELECT CASE WHEN EXISTS (
    SELECT 1 FROM bookings 
    WHERE room_id = :roomId 
      AND status NOT IN ('CANCELLED', 'CHECKED_OUT', 'NO_SHOW')
      AND check_in_date < :requestedEndDate 
      AND check_out_date > :requestedStartDate
) THEN 1 ELSE 0 END;
```

### Insert Room Status Block
```sql
INSERT INTO room_status_blocks (
    id, room_id, property_id, status, start_date, end_date, reason, created_by, created_at, updated_at, active
) VALUES (
    :id, :roomId, :propertyId, :status, :startDate, :endDate, :reason, :managerId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true
);
```
