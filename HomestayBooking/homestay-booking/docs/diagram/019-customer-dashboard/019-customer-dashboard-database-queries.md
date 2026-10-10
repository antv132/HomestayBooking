# 019 - Customer Dashboard: Database Queries

## 1. Top Row Counter Queries

### Active Bookings
```sql
-- BookingRepository.countActiveByCustomerId
SELECT COUNT(1)
FROM bookings
WHERE customer_id = :customerId
  AND status IN ('CONFIRMED', 'CHECKED_IN');
```

### Pending Actions
```sql
-- BookingRepository.countPendingDepositByCustomerId
SELECT COUNT(1)
FROM bookings
WHERE customer_id = :customerId
  AND status = 'PENDING_DEPOSIT';

-- PaymentRepository.countByCustomerIdAndStatus
SELECT COUNT(1)
FROM payments
WHERE customer_id = :customerId
  AND status = 'PENDING';
```

### Open Maintenance Tickets
```sql
-- MaintenanceTicketRepository.countByCustomerIdAndStatusIn
SELECT COUNT(1)
FROM maintenance_tickets
WHERE customer_id = :customerId
  AND status IN ('OPEN', 'IN_PROGRESS');
```

### Unread Notifications
```sql
-- NotificationRepository.countByUserIdAndIsReadFalse
SELECT COUNT(1)
FROM notifications
WHERE user_id = :customerId
  AND is_read = FALSE;
```

---

## 2. Highlight Events Queries

### Next Check-In
```sql
-- BookingRepository.findFirstByCustomerIdAndStatusAndCheckInDateGreaterThanEqualOrderByCheckInDateAsc
SELECT *
FROM bookings
WHERE customer_id = :customerId
  AND status = 'CONFIRMED'
  AND check_in_date >= :today
ORDER BY check_in_date ASC
LIMIT 1;
```

### Next Check-Out
```sql
-- BookingRepository.findFirstByCustomerIdAndStatusAndCheckOutDateGreaterThanEqualOrderByCheckOutDateAsc
SELECT *
FROM bookings
WHERE customer_id = :customerId
  AND status = 'CHECKED_IN'
  AND check_out_date >= :today
ORDER BY check_out_date ASC
LIMIT 1;
```

---

## 3. Recent Activity List Queries

### Top 5 Upcoming Bookings
```sql
-- BookingRepository.findUpcomingByCustomerId
SELECT *
FROM bookings
WHERE customer_id = :customerId
  AND check_in_date >= :today
ORDER BY check_in_date ASC
LIMIT 5;
```

### Top 3 Recent Payments
```sql
-- PaymentRepository.findByCustomerIdOrderByCreatedAtDesc
SELECT *
FROM payments
WHERE customer_id = :customerId
ORDER BY created_at DESC
LIMIT 3;
```

### Top 5 Recent Notifications
```sql
-- NotificationRepository.findByUserIdOrderByCreatedAtDesc
SELECT *
FROM notifications
WHERE user_id = :customerId
ORDER BY created_at DESC
LIMIT 5;
```
