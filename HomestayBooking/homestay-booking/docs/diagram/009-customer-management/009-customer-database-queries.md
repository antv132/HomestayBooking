# 009 - Customer Management: Database Queries

## 1. User Directory — List & Filter

### List Users by Role with Filters (Admin & Manager)
```sql
-- UserRepository.findByRoleWithFilters
SELECT u.*
FROM users u
WHERE u.role = :role
  AND (:status IS NULL OR u.status = :status)
  AND (
        :keyword IS NULL
        OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
  )
ORDER BY u.created_at DESC
OFFSET :offset LIMIT :limit;
```

### Batch Load Booking Counts (N+1 prevention)
```sql
-- BookingRepository.countBookingsByCustomerIds
SELECT b.customer_id, COUNT(b.id)
FROM bookings b
WHERE b.customer_id IN (:customerIds)
GROUP BY b.customer_id;
```

### Batch Load Total Spend (N+1 prevention)
```sql
-- PaymentRepository.sumPaidAmountByCustomerIds
SELECT p.customer_id, SUM(p.amount)
FROM payments p
WHERE p.customer_id IN (:customerIds)
  AND p.status = 'PAID'
GROUP BY p.customer_id;
```

## 2. Customer Booking History (Admin Drawer)

### Find Customer Bookings with Room & Property Detail
```sql
-- BookingRepository.findByCustomerIdWithDetails
SELECT b.*, r.room_number, pr.name AS property_name
FROM bookings b
JOIN rooms r ON b.room_id = r.id
JOIN properties pr ON r.property_id = pr.id
WHERE b.customer_id = :customerId
ORDER BY b.created_at DESC
OFFSET :offset LIMIT :limit;
```

## 3. Status Update — Suspend / Activate (FR-09)

### Update Customer Status
```sql
UPDATE users
SET status = :newStatus, updated_at = CURRENT_TIMESTAMP
WHERE id = :userId
  AND role = 'CUSTOMER';
```

### Revoke All Refresh Tokens on Suspend
```sql
-- RefreshTokenRepository.revokeAllByUser
UPDATE refresh_tokens
SET revoked = true, updated_at = CURRENT_TIMESTAMP
WHERE user_id = :userId
  AND revoked = false;
```

## 4. Profile Self-Service

### Update Profile
```sql
UPDATE users
SET full_name  = :fullName,
    phone      = :phone,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :userId;
```

### Change Password
```sql
-- Verify current password first (in-memory bcrypt check)
-- Then update:
UPDATE users
SET password_hash = :newHash,
    updated_at    = CURRENT_TIMESTAMP
WHERE id = :userId;
```

## 5. Manager — Customer Directory

### Manager List Active Customers
```sql
SELECT u.*,
       COUNT(b.id)   AS booking_count
FROM users u
LEFT JOIN bookings b ON b.customer_id = u.id
WHERE u.role = 'CUSTOMER'
  AND (:status IS NULL OR u.status = :status)
  AND (
        :search IS NULL
        OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
  )
GROUP BY u.id
ORDER BY u.created_at DESC
OFFSET :offset LIMIT :limit;
```
