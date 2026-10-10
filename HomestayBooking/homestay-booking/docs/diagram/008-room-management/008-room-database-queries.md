# 008 - Room Management: Database Queries

## 1. Room Soft Delete & Validation

### Delete Guard (Check Blocking Bookings)
Ensures a room cannot be soft-deleted if it has active or future blocking bookings.
```sql
SELECT COUNT(1) 
FROM bookings 
WHERE room_id = :roomId 
  AND status IN (
      'PENDING_DEPOSIT', 
      'CONFIRMED', 
      'CHECKED_IN', 
      'PENDING_INSPECTION', 
      'PENDING_DAMAGE_PAYMENT'
  );
-- Must return 0 to allow soft delete
```

### Soft-Delete a Room
```sql
UPDATE rooms
SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
WHERE id = :roomId AND deleted_at IS NULL;
```

### Validate Active Room Number Uniqueness (Insert/Update)
```sql
SELECT CASE WHEN EXISTS (
    SELECT 1 FROM rooms 
    WHERE property_id = :propertyId 
      AND room_number = :roomNumber 
      AND deleted_at IS NULL
      AND id != :excludeRoomId -- Used during update
) THEN 1 ELSE 0 END;
```

## 2. List & Filter Queries

### Manager List Active Rooms
```sql
SELECT r.* 
FROM rooms r
WHERE r.property_id = :propertyId
  AND r.deleted_at IS NULL
  AND (:floorId IS NULL OR r.floor_id = :floorId)
  AND (:status IS NULL OR r.status = :status)
  AND (:roomType IS NULL OR r.room_type = :roomType)
ORDER BY r.created_at DESC
OFFSET :offset LIMIT :limit;
```

## 3. Gallery Management

### Validate Max 10 Images
```sql
SELECT COUNT(1) FROM room_images WHERE room_id = :roomId;
```

### Set Primary Image Transaction
```sql
-- Step 1: Unset all
UPDATE room_images 
SET is_primary = 0 
WHERE room_id = :roomId;

-- Step 2: Set target
UPDATE room_images 
SET is_primary = 1 
WHERE id = :imageId;
```
