# 016 - Reporting & Analytics: Database Queries

## 1. Revenue Report (Based on Payments)

### Sum Revenue by Type
```sql
-- PaymentRepository.sumRevenueByType
SELECT SUM(p.amount)
FROM payments p
JOIN bookings b ON p.booking_id = b.id
JOIN rooms r ON b.room_id = r.id
WHERE p.status = 'COMPLETED'
  AND (:type IS NULL OR p.type = :type)
  AND (:propertyId IS NULL OR r.property_id = :propertyId)
  AND (:from IS NULL OR p.paid_at >= :from)
  AND (:to IS NULL OR p.paid_at <= :to);
```

### Fetch Raw Revenue Data for Bucketing
```sql
-- PaymentRepository.findRevenueRawData
SELECT 
    p.paid_at, 
    p.amount, 
    p.type, 
    prop.id, 
    prop.name, 
    b.id 
FROM payments p
JOIN bookings b ON p.booking_id = b.id
JOIN rooms r ON b.room_id = r.id
JOIN properties prop ON r.property_id = prop.id
WHERE p.status = 'COMPLETED'
  AND (:propertyId IS NULL OR r.property_id = :propertyId)
  AND (:from IS NULL OR p.paid_at >= :from)
  AND (:to IS NULL OR p.paid_at <= :to)
ORDER BY p.paid_at ASC;
```

---

## 2. Occupancy Report (Based on Bookings)

### Count Total Rooms for Property
```sql
-- RoomRepository.countByPropertyId
SELECT COUNT(1) 
FROM rooms 
WHERE property_id = :propertyId;
```

### Fetch Relevant Bookings for Occupancy
```sql
-- BookingRepository.findOccupancyRawData
SELECT b.check_in_date, b.check_out_date, b.status 
FROM bookings b
JOIN rooms r ON b.room_id = r.id
WHERE r.property_id = :propertyId
  AND b.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
  AND b.check_in_date < :rangeEnd
  AND b.check_out_date > :rangeStart;
-- Explanation: Finds bookings that overlap with the requested time range.
```

---

## 3. Booking Trend Report

### Fetch Booking Creation Times
```sql
-- BookingRepository.findBookingCreationTimes
SELECT b.created_at
FROM bookings b
JOIN rooms r ON b.room_id = r.id
WHERE r.property_id = :propertyId
  AND b.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
  AND b.created_at >= :rangeStart
  AND b.created_at <= :rangeEnd;
```

### Count Breakdown by Status
```sql
-- BookingRepository.countByStatusForPropertyInRange
SELECT b.status, COUNT(b.id)
FROM bookings b
JOIN rooms r ON b.room_id = r.id
WHERE r.property_id = :propertyId
  AND b.created_at >= :rangeStart
  AND b.created_at <= :rangeEnd
GROUP BY b.status;
```
