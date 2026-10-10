# 021 - Housekeeping Management: Database Queries

## 1. Auto-Creation & Validation (Manager / System)

### Check if Task Exists for Booking
```sql
-- HousekeepingTaskRepository.existsByBooking_Id
SELECT COUNT(1) > 0
FROM housekeeping_tasks
WHERE booking_id = :bookingId;
```

### Check if Room already has an Open Task
```sql
-- HousekeepingTaskRepository.findOpenByRoomId
SELECT *
FROM housekeeping_tasks
WHERE room_id = :roomId
  AND status IN ('PENDING', 'IN_PROGRESS')
LIMIT 1;
```

---

## 2. Manager Scheduling & Assignment

### Fetch Schedule Grid
```sql
-- HousekeepingTaskRepository.findScheduleTasks
SELECT t.*
FROM housekeeping_tasks t
JOIN rooms r ON t.room_id = r.id
WHERE r.property_id = :propertyId
  AND t.status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED')
  AND t.created_at >= :startOfDay
  AND t.created_at < :endOfDay;
```

### Assign Employee to Task
```sql
-- Managed by Spring Data JPA
UPDATE housekeeping_tasks
SET assigned_employee_id = :employeeId,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :taskId;
```

---

## 3. Employee Execution

### Fetch Employee's Tasks
```sql
-- HousekeepingTaskRepository.findForEmployee
SELECT t.*
FROM housekeeping_tasks t
WHERE t.assigned_employee_id = :employeeId
  AND (:status IS NULL OR t.status = :status)
ORDER BY t.created_at ASC
OFFSET :offset LIMIT :limit;
```

### Employee Starts Task
```sql
-- 1. Update Task
UPDATE housekeeping_tasks
SET status = 'IN_PROGRESS',
    started_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :taskId AND assigned_employee_id = :employeeId AND status = 'PENDING';

-- 2. Sync Room Status
UPDATE rooms
SET status = 'CLEANING_IN_PROGRESS',
    updated_at = CURRENT_TIMESTAMP
WHERE id = :roomId;
```

### Employee Finishes Task
```sql
-- 1. Update Task
UPDATE housekeeping_tasks
SET status = 'COMPLETED',
    completed_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :taskId AND assigned_employee_id = :employeeId AND status = 'IN_PROGRESS';

-- 2. Sync Room Status
UPDATE rooms
SET status = 'AVAILABLE',
    updated_at = CURRENT_TIMESTAMP
WHERE id = :roomId;
```
