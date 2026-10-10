# 013 - Maintenance Management: Database Queries

## 1. Ticket Listing Queries

### Customer: List My Active Tickets
```sql
-- MaintenanceTicketRepository.findByCustomerPaged
SELECT t.*
FROM maintenance_tickets t
WHERE t.customer_id = :customerId
  AND t.deleted_at IS NULL
  AND (:status IS NULL OR t.status = :status)
ORDER BY t.created_at DESC
OFFSET :offset LIMIT :limit;
```

### Manager: List Property Tasks (SCR-41)
```sql
-- MaintenanceTicketRepository.findForManagerBoard
SELECT t.*
FROM maintenance_tickets t
JOIN rooms r ON t.room_id = r.id
WHERE r.property_id = :propertyId
  AND t.deleted_at IS NULL
  AND (:status IS NULL OR t.status = :status)
  AND (
      :search IS NULL
      OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))
      OR LOWER(t.customer.full_name) LIKE LOWER(CONCAT('%', :search, '%'))
      OR LOWER(r.room_number) LIKE LOWER(CONCAT('%', :search, '%'))
  )
ORDER BY
    CASE t.status
        WHEN 'OPEN' THEN 1
        WHEN 'ASSIGNED' THEN 2
        WHEN 'IN_PROGRESS' THEN 3
        WHEN 'RESOLVED' THEN 4
        WHEN 'CLOSED' THEN 5
        ELSE 6
    END,
    t.created_at DESC
OFFSET :offset LIMIT :limit;
```

### Employee: List Assigned Tasks (SCR-61)
```sql
-- MaintenanceTicketRepository.findForEmployee
SELECT t.*
FROM maintenance_tickets t
WHERE t.assigned_employee_id = :employeeId
  AND t.deleted_at IS NULL
  AND (:status IS NULL OR t.status = :status)
ORDER BY
    CASE t.status
        WHEN 'ASSIGNED' THEN 1
        WHEN 'IN_PROGRESS' THEN 2
        WHEN 'RESOLVED' THEN 3
        ELSE 4
    END,
    t.created_at DESC
OFFSET :offset LIMIT :limit;
```

---

## 2. Ticket Modification Queries

### Soft Delete (Customer)
```sql
UPDATE maintenance_tickets
SET deleted_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :ticketId
  AND customer_id = :customerId
  AND status = 'OPEN';
```

### Assign Task (Manager)
```sql
UPDATE maintenance_tickets
SET assigned_employee_id = :assigneeId,
    status = CASE WHEN status = 'OPEN' THEN 'ASSIGNED' ELSE status END,
    assigned_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :ticketId;
```

### Update Status (Employee)
```sql
UPDATE maintenance_tickets
SET status = :newStatus, -- 'IN_PROGRESS' or 'RESOLVED'
    work_note = :workNote, -- Employee internal note
    updated_at = CURRENT_TIMESTAMP
WHERE id = :ticketId
  AND assigned_employee_id = :employeeId;
```

### Verify & Close (Manager)
```sql
UPDATE maintenance_tickets
SET status = 'CLOSED',
    verified_by = :managerId,
    verified_at = CURRENT_TIMESTAMP,
    resolution_note = :resolutionNote,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :ticketId
  AND status = 'RESOLVED';
```

---

## 3. Constraint Validations

### Validate Active Booking for New Ticket
```sql
-- BookingRepository.findActiveWithRoomPropertyByCustomerId
SELECT b.*
FROM bookings b
WHERE b.customer_id = :customerId
  AND b.status IN ('CONFIRMED', 'CHECKED_IN');
-- Customer can only create a ticket if they have a matching active booking for the room
```

### Validate Employee Scope for Assignment
```sql
-- EmployeePropertyAssignmentRepository.existsByEmployeeIdAndPropertyIdAndStatus
SELECT COUNT(1) > 0
FROM employee_property_assignments
WHERE employee_id = :employeeId
  AND property_id = :propertyId
  AND status = 'ACTIVE';
```
