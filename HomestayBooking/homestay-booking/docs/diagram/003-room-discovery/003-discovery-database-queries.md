# 003 - Room Discovery: Database Queries

This document provides the detailed SQL used in implementing the Room Discovery functions.

## 1. Search & Filter Rooms (`GET /api/v1/rooms`)

### Base Search Query (With Pagination & Filters)
```sql
SELECT r.id, r.room_number, r.room_type, r.price_per_night, r.capacity, r.area,
       p.name AS property_name, p.id AS property_id,
       ri.image_url AS primary_image_url
FROM rooms r
JOIN properties p ON r.property_id = p.id
LEFT JOIN room_images ri ON r.id = ri.room_id AND ri.is_primary = true
WHERE p.status = 'ACTIVE'
  AND r.status NOT IN ('MAINTENANCE', 'OUT_OF_SERVICE')
  -- Dynamic Filters appended based on user input:
  AND r.capacity >= :requestedCapacity
  AND r.price_per_night BETWEEN :minPrice AND :maxPrice
  AND r.room_type = :roomType
  AND p.address ILIKE :locationKeyword
  -- Overlap check (if dates provided)
  AND NOT EXISTS (
      SELECT 1 FROM bookings b
      WHERE b.room_id = r.id
        AND b.status IN ('PENDING_DEPOSIT', 'CONFIRMED', 'RESERVED', 'OCCUPIED')
        AND b.check_in_date < :checkOutDate 
        AND b.check_out_date > :checkInDate
  )
ORDER BY r.price_per_night ASC
OFFSET :offset LIMIT :limit;
```

## 2. View Room Details (`GET /api/v1/rooms/{id}`)

### Fetch Room & Property details
```sql
SELECT r.id, r.room_number, r.room_type, r.price_per_night, r.capacity, r.area, r.description, r.amenities,
       p.id AS property_id, p.name AS property_name, p.address AS property_address
FROM rooms r
JOIN properties p ON r.property_id = p.id
WHERE r.id = :roomId 
  AND p.status = 'ACTIVE';
```

### Fetch Gallery Images
```sql
SELECT id, image_url, is_primary, sort_order
FROM room_images
WHERE room_id = :roomId
ORDER BY sort_order ASC;
```

## 3. Availability Calendar (`GET /api/v1/rooms/{id}/availability`)

### Fetch overlapping bookings for a given month
```sql
SELECT check_in_date, check_out_date, status
FROM bookings
WHERE room_id = :roomId
  AND status IN ('PENDING_DEPOSIT', 'CONFIRMED', 'RESERVED', 'OCCUPIED')
  AND check_in_date <= :endOfMonth
  AND check_out_date >= :startOfMonth;
```

## 4. Landing Page Components (SCR-01)

### Fetch Active Promotions
```sql
SELECT id, title, subtitle, image_url, cta_url, color_theme
FROM promotions
WHERE is_active = true
ORDER BY sort_order ASC;
```

### Fetch Search Suggestions (Debounced /public/suggestions)
```sql
SELECT id, name AS suggestion_text, 'PROPERTY' as type
FROM properties
WHERE status = 'ACTIVE' AND name ILIKE :keyword
UNION
SELECT DISTINCT address AS suggestion_text, 'LOCATION' as type
FROM properties
WHERE status = 'ACTIVE' AND address ILIKE :keyword
LIMIT 10;
```
