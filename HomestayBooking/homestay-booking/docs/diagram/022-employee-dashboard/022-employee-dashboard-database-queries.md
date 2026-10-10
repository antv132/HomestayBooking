# 022 - Employee Dashboard: Database Queries

## 1. Property Assignment Validation
```sql
-- EmployeePropertyAssignmentRepository.findPropertyIdsByEmployeeIdAndStatus
SELECT property_id
FROM employee_property_assignments
WHERE employee_id = :employeeId
  AND status = 'ACTIVE';
```

## 2. Housekeeping Queries
```sql
-- Count Pending/In-Progress
SELECT COUNT(1) FROM housekeeping_tasks
WHERE assigned_employee_id = :employeeId
  AND status IN ('PENDING', 'IN_PROGRESS');

-- Count Completed Today
SELECT COUNT(1) FROM housekeeping_tasks
WHERE assigned_employee_id = :employeeId
  AND status = 'COMPLETED'
  AND completed_at >= :dayStart
  AND completed_at < :dayEnd;

-- Find Today's Tasks
SELECT * FROM housekeeping_tasks
WHERE assigned_employee_id = :employeeId
  AND (
      status IN ('PENDING', 'IN_PROGRESS') 
      OR (status = 'COMPLETED' AND completed_at >= :dayStart AND completed_at < :dayEnd)
  )
ORDER BY created_at DESC;
```

## 3. Maintenance Queries
```sql
-- Count Pending/In-Progress
SELECT COUNT(1) FROM maintenance_tickets
WHERE assigned_employee_id = :employeeId
  AND status IN ('ASSIGNED', 'IN_PROGRESS');

-- Count Completed Today
SELECT COUNT(1) FROM maintenance_tickets
WHERE assigned_employee_id = :employeeId
  AND status = 'RESOLVED'
  AND updated_at >= :dayStart
  AND updated_at < :dayEnd;

-- Find Today's Tasks
SELECT * FROM maintenance_tickets
WHERE assigned_employee_id = :employeeId
  AND (
      status IN ('ASSIGNED', 'IN_PROGRESS') 
      OR (status = 'RESOLVED' AND updated_at >= :dayStart AND updated_at < :dayEnd)
  )
ORDER BY created_at DESC;
```

## 4. Room Inspection Queries
```sql
-- Count Pending/In-Progress
-- Note: Room inspections might be filtered by property if not explicitly assigned to a single employee yet
SELECT COUNT(1) FROM room_inspections ri
JOIN rooms r ON ri.room_id = r.id
WHERE r.property_id IN (:propertyIds)
  AND (ri.assigned_employee_id = :employeeId OR ri.assigned_employee_id IS NULL)
  AND ri.status IN ('PENDING', 'IN_PROGRESS');

-- Count Completed Today
SELECT COUNT(1) FROM room_inspections ri
WHERE ri.assigned_employee_id = :employeeId
  AND ri.status IN ('COMPLETED', 'DAMAGE_REPORTED')
  AND ri.updated_at >= :dayStart
  AND ri.updated_at < :dayEnd;
```
*(Note: Exact joins might vary slightly depending on JPA annotations, but the logic remains grouping by `assigned_employee_id` and specific statuses).*
