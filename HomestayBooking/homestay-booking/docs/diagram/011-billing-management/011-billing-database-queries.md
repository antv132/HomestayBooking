# 011 - Billing (Invoice) Management: Database Queries

## 1. Invoice Queries (Read)

### Customer: List My Invoices
```sql
-- InvoiceRepository.findByCustomer_Id[AndStatus]
SELECT i.*
FROM invoices i
WHERE i.customer_id = :customerId
  AND (:status IS NULL OR i.status = :status)
ORDER BY i.created_at DESC
OFFSET :offset LIMIT :limit;
```

### Manager: List Invoices by Property
```sql
-- InvoiceRepository.findByPropertyIdAndOptionalStatus
SELECT i.*
FROM invoices i
WHERE i.property_id = :propertyId
  AND (:status IS NULL OR i.status = :status)
ORDER BY i.created_at DESC
OFFSET :offset LIMIT :limit;
```

### Booking Breakdown: Fetch Invoices for Booking
```sql
-- InvoiceRepository.findByBooking_IdOrderByTypeAsc
SELECT i.*
FROM invoices i
WHERE i.booking_id = :bookingId
ORDER BY i.type ASC; -- DEPOSIT first, then REMAINING_BALANCE
```

---

## 2. Invoice Issuance (Write)

### Check Existence (Idempotency Guard)
```sql
-- InvoiceRepository.existsByBooking_IdAndType
SELECT COUNT(1) > 0
FROM invoices
WHERE booking_id = :bookingId
  AND type = :type; -- 'DEPOSIT' or 'REMAINING_BALANCE'
```

### Insert Invoice
```sql
INSERT INTO invoices (
    id, booking_id, customer_id, property_id,
    type, amount, status, due_date,
    created_at, updated_at
) VALUES (
    gen_random_uuid(), :bookingId, :customerId, :propertyId,
    :type, :amount, 'UNPAID', :dueDate,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
```

---

## 3. Invoice Status Sync (from Payment webhook)

### Fallback Match by Booking + Type
```sql
-- Used when Payment doesn't have an invoice_id yet
-- InvoiceRepository.findByBooking_IdAndType
SELECT i.*
FROM invoices i
WHERE i.booking_id = :bookingId
  AND i.type = :type;
```

### Mark PENDING_PAYMENT (or FAILED)
```sql
UPDATE invoices
SET status = :newStatus, -- 'PENDING_PAYMENT' or 'UNPAID'
    updated_at = CURRENT_TIMESTAMP
WHERE id = :invoiceId
  AND status != 'PAID'
  AND status != 'CANCELLED';
```

### Mark PAID
```sql
UPDATE invoices
SET status = 'PAID',
    paid_at = :paidAt, -- usually CURRENT_TIMESTAMP
    updated_at = CURRENT_TIMESTAMP
WHERE id = :invoiceId
  AND status != 'PAID'
  AND status != 'CANCELLED';
```
