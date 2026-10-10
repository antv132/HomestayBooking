# 014 - Review & Rating Management: Database Queries

## 1. Review Listing Queries

### Customer: List My Reviews
```sql
-- ReviewRepository.findByCustomer_IdOrderByCreatedAtDesc
SELECT r.*
FROM reviews r
WHERE r.customer_id = :customerId
ORDER BY r.created_at DESC
OFFSET :offset LIMIT :limit;
```

### Manager: List Property Reviews
```sql
-- ReviewRepository.findByPropertyIdAndOptionalStatus
SELECT r.*
FROM reviews r
JOIN rooms room ON r.room_id = room.id
WHERE room.property_id = :propertyId
  AND (:status IS NULL OR r.status = :status)
ORDER BY r.created_at DESC
OFFSET :offset LIMIT :limit;
```

### Admin: List All Reviews
```sql
-- ReviewRepository.findAllByOptionalStatus
SELECT r.*
FROM reviews r
WHERE (:status IS NULL OR r.status = :status)
ORDER BY r.created_at DESC
OFFSET :offset LIMIT :limit;
```

---

## 2. Review Modification Queries

### Insert Review (Customer)
```sql
INSERT INTO reviews (
    id, booking_id, customer_id, room_id,
    rating, comment, status,
    created_at, updated_at
) VALUES (
    gen_random_uuid(), :bookingId, :customerId, :roomId,
    :rating, :comment, 'PUBLISHED',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
```

### Moderate Review (Manager/Admin)
```sql
UPDATE reviews
SET status = :newStatus, -- 'HIDDEN' or 'PUBLISHED'
    moderated_by = :actorId,
    moderated_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :reviewId;
```

---

## 3. Rating Aggregation Queries (Triggered on Change)

### Calculate Average Rating
```sql
-- ReviewRepository.averageRatingByRoomId
SELECT AVG(CAST(r.rating AS DOUBLE PRECISION))
FROM reviews r
WHERE r.room_id = :roomId
  AND r.status = 'PUBLISHED';
```

### Count Published Reviews
```sql
-- ReviewRepository.countPublishedByRoomId
SELECT COUNT(1)
FROM reviews r
WHERE r.room_id = :roomId
  AND r.status = 'PUBLISHED';
```

### Update Room Cache
```sql
-- RoomRepository.save (triggered by RoomRatingAggregateService)
UPDATE rooms
SET average_rating = :averageRating,
    total_reviews = :totalReviews,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :roomId;
```

---

## 4. Constraint Validations

### Check if Booking Already Reviewed
```sql
-- ReviewRepository.existsByBooking_Id
SELECT COUNT(1) > 0
FROM reviews
WHERE booking_id = :bookingId;
```

### Check Booking Status (Before Reviewing)
```sql
SELECT b.status
FROM bookings b
WHERE b.id = :bookingId;
-- Must be 'CHECKED_OUT'
```
