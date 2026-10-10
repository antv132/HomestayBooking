# 017 - Administration: Database Queries

## 1. Directory Listing with Aggregates

### List Users with Filters
```sql
-- UserRepository.findByRoleWithFilters
SELECT u.* 
FROM users u
WHERE u.role = :role
  AND (:status IS NULL OR u.status = :status)
  AND (
      :search IS NULL
      OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', :search, '%'))
      OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
      OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%'))
  )
ORDER BY u.created_at DESC
OFFSET :offset LIMIT :limit;
```

### Aggregates for Manager Directory
```sql
-- ManagerPropertyAssignmentRepository.countActiveAssignmentsByManagerIds
SELECT manager_id, COUNT(id)
FROM manager_property_assignments
WHERE manager_id IN (:managerIds)
  AND status = 'ACTIVE'
GROUP BY manager_id;
```

### Aggregates for Customer Directory
```sql
-- BookingRepository.countBookingsByCustomerIds
SELECT customer_id, COUNT(id)
FROM bookings
WHERE customer_id IN (:customerIds)
GROUP BY customer_id;

-- PaymentRepository.sumPaidAmountByCustomerIds
SELECT b.customer_id, SUM(p.amount)
FROM payments p
JOIN bookings b ON p.booking_id = b.id
WHERE b.customer_id IN (:customerIds)
  AND p.status = 'COMPLETED'
GROUP BY b.customer_id;
```

---

## 2. Property Manager Assignment

### Check if Property has Active Manager (Before Activation)
```sql
-- ManagerPropertyAssignmentRepository.existsByPropertyIdAndStatus
SELECT COUNT(1) > 0
FROM manager_property_assignments
WHERE property_id = :propertyId
  AND status = 'ACTIVE';
```

### Replace Manager Assignment
```sql
-- 1. Find Current Active
SELECT * FROM manager_property_assignments 
WHERE property_id = :propertyId AND status = 'ACTIVE';

-- 2. Deactivate Old
UPDATE manager_property_assignments 
SET status = 'INACTIVE' 
WHERE id = :oldAssignmentId;

-- 3. Insert New
INSERT INTO manager_property_assignments (
    id, manager_id, property_id, assigned_by, assigned_at, status
) VALUES (
    gen_random_uuid(), :newManagerId, :propertyId, :adminId, CURRENT_TIMESTAMP, 'ACTIVE'
);
```

---

## 3. System Settings

### Fetch System Settings
```sql
-- SystemSettingRepository.findByKeyIn
SELECT * FROM system_settings
WHERE key IN (
    'DEPOSIT_PERCENTAGE', 'CANCEL_TIMEOUT_HOURS', 'SYSTEM_NAME', 
    'SUPPORT_EMAIL', 'BANK_ACCOUNT_NUMBER', 'BANK_ACCOUNT_NAME', 'BANK_NAME'
);
```

### Upsert System Setting
```sql
-- Managed by Spring Data JPA save()
-- Check if exists:
SELECT * FROM system_settings WHERE key = :key;

-- Update:
UPDATE system_settings 
SET value = :value, updated_by = :adminId, updated_at = CURRENT_TIMESTAMP
WHERE key = :key;

-- Or Insert:
INSERT INTO system_settings (key, value, description, updated_by)
VALUES (:key, :value, :description, :adminId);
```

---

## 4. User Suspension

### Suspend Customer & Revoke Tokens
```sql
-- 1. Update Status
UPDATE users SET status = 'SUSPENDED' WHERE id = :customerId;

-- 2. Revoke Refresh Tokens
-- RefreshTokenRepository.revokeAllByUser
UPDATE refresh_tokens 
SET revoked = TRUE 
WHERE user_id = :customerId AND revoked = FALSE;
```
