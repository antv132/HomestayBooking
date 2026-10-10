# 012 - Payment Management: Database Queries

## 1. VNPay Pre-Checkout Queries (Write / Validate)

### Check Duplicate Paid Payments
```sql
-- PaymentRepository.findByBookingIdOrderByCreatedAtDesc
SELECT p.*
FROM payments p
WHERE p.booking_id = :bookingId
ORDER BY p.created_at DESC;
-- Java iterates over result to ensure no payment has type == requestedType AND status == 'PAID'
```

### Insert PENDING Payment
```sql
INSERT INTO payments (
    id, booking_id, customer_id, invoice_id,
    type, method, status, amount, order_ref,
    created_at, updated_at
) VALUES (
    :id, :bookingId, :customerId, :invoiceId,
    :type, 'VNPAY', 'PENDING', :amount, :id, -- orderRef defaults to ID initially
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
```

---

## 2. VNPay Return Queries (Webhook)

### Resolve Payment by ID or VNPTxnRef
```sql
-- Resolving by string UUID parsing first. If failed, falls back to orderRef
-- PaymentRepository.findByOrderRef
SELECT p.*
FROM payments p
WHERE p.order_ref = :orderRef;
```

### Complete Payment (Mark PAID)
```sql
UPDATE payments
SET status = 'PAID',
    gateway_transaction_id = :vnpTransactionNo,
    gateway_response_code = '00',
    paid_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :paymentId;
```

### Apply Side Effects (Deposit → Confirmed)
```sql
UPDATE bookings
SET status = 'CONFIRMED',
    updated_at = CURRENT_TIMESTAMP
WHERE id = :bookingId;
```

---

## 3. Manager Queries (List & Filter)

### List Payments Scoped to Assigned Properties (SCR-36)
```sql
-- PaymentRepository.findForManagerWithFilters
SELECT p.*
FROM payments p
JOIN bookings b ON p.booking_id = b.id
JOIN rooms r ON b.room_id = r.id
WHERE r.property_id IN (:propertyIds)
  AND (:status IS NULL OR p.status = :status)
  AND (:type IS NULL OR p.type = :type)
  AND (:method IS NULL OR p.method = :method)
  AND (
      :search IS NULL
      OR LOWER(CAST(p.id AS VARCHAR)) LIKE LOWER(CONCAT('%', :search, '%'))
      OR LOWER(p.customer.full_name) LIKE LOWER(CONCAT('%', :search, '%'))
      OR LOWER(p.gateway_transaction_id) LIKE LOWER(CONCAT('%', :search, '%'))
  )
ORDER BY p.created_at DESC
OFFSET :offset LIMIT :limit;
```

---

## 4. Constraint Validations

### Check Damage Report Status (US6)
```sql
-- DamageReportRepository.findFirstByBooking_IdOrderByCreatedAtDesc
SELECT d.*
FROM damage_reports d
WHERE d.booking_id = :bookingId
ORDER BY d.created_at DESC
LIMIT 1;
-- Service validates if d.status == 'DISPUTED' -> throw exception
```
