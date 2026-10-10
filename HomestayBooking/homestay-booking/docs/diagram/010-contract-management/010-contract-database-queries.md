# 010 - Contract Management: Database Queries

## 1. Contract Creation (Outbox Worker — Idempotent)

### Find Existing Contract for Booking (before create)
```sql
-- ContractRepository.findByBookingId
SELECT c.*
FROM contracts c
WHERE c.booking_id = :bookingId;
-- Returns Optional<Contract> — if present, skip INSERT (idempotent)
```

### Insert New Contract
```sql
INSERT INTO contracts (
    id, booking_id, customer_id, room_id,
    check_in_date, check_out_date,
    deposit_amount, total_amount,
    status, generated_at, created_at, updated_at
) VALUES (
    gen_random_uuid(), :bookingId, :customerId, :roomId,
    :checkInDate, :checkOutDate,
    :depositAmount, :totalAmount,
    'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
```

### Update PDF URL after Generation
```sql
UPDATE contracts
SET pdf_url      = :pdfUrl,
    generated_at = CURRENT_TIMESTAMP,
    updated_at   = CURRENT_TIMESTAMP
WHERE id = :contractId;
```

### Update sent_at after Email Sent
```sql
UPDATE contracts
SET sent_at    = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :contractId;
```

---

## 2. List Queries

### Customer: My Contracts with Filters
```sql
-- ContractRepository.findByCustomerWithFilters
SELECT c.*
FROM contracts c
WHERE c.customer_id = :customerId
  AND (:status IS NULL OR c.status = :status)
  AND (
        :search IS NULL
        OR LOWER(CAST(c.booking_id AS VARCHAR)) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(c.room.room_number) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(c.room.property.name) LIKE LOWER(CONCAT('%', :search, '%'))
  )
ORDER BY c.generated_at DESC
OFFSET :offset LIMIT :limit;
```

### Manager: Contracts by Assigned Properties
```sql
-- ContractRepository.findByPropertyIdsWithFilters
SELECT c.*
FROM contracts c
JOIN rooms r ON c.room_id = r.id
WHERE r.property_id IN (:propertyIds)
  AND (:status IS NULL OR c.status = :status)
  AND (
        :search IS NULL
        OR LOWER(c.customer.full_name) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(CAST(c.booking_id AS VARCHAR)) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(r.room_number) LIKE LOWER(CONCAT('%', :search, '%'))
  )
ORDER BY c.generated_at DESC
OFFSET :offset LIMIT :limit;
```

### Admin: All Contracts (unscoped)
```sql
-- ContractRepository.findAllWithFilters
SELECT c.*
FROM contracts c
WHERE (:status IS NULL OR c.status = :status)
  AND (
        :search IS NULL
        OR LOWER(c.customer.full_name) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(CAST(c.booking_id AS VARCHAR)) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(c.room.room_number) LIKE LOWER(CONCAT('%', :search, '%'))
  )
ORDER BY c.generated_at DESC
OFFSET :offset LIMIT :limit;
```

---

## 3. Manager Scope Validation

### Check Active Property Assignment
```sql
-- ManagerPropertyAssignmentRepository.findActivePropertiesByManagerId
SELECT p.*
FROM manager_property_assignments mpa
JOIN properties p ON mpa.property_id = p.id
WHERE mpa.manager_id = :managerId
  AND mpa.status = 'ACTIVE';
```

### Validate Single Property Access
```sql
SELECT COUNT(1)
FROM manager_property_assignments
WHERE manager_id = :managerId
  AND property_id = :propertyId
  AND status = 'ACTIVE';
-- Must return > 0 else ForbiddenException
```

---

## 4. Outbox Events (Contract-related)

```sql
-- Outbox events inserted within the same transaction as trigger actions
INSERT INTO outbox_events (id, event_type, payload, status, created_at)
VALUES (
    gen_random_uuid(),
    'CONTRACT_GENERATE',      -- or CONTRACT_EMAIL_SEND / CONTRACT_RESEND
    :jsonPayload,             -- {bookingId, actorId} or {contractId, targetEmail, actorId}
    'PENDING',
    CURRENT_TIMESTAMP
);
```
