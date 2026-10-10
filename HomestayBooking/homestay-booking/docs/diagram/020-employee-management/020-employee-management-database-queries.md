# 020 - Employee Management: Database Queries

## 1. Finding Unassigned Employees

### Fetch Unassigned Employees
```sql
-- EmployeePropertyAssignmentRepository.findUnassignedEmployees
SELECT u.*
FROM users u
WHERE u.role = :role 
  AND u.status = :status
  AND (
      :search IS NULL
      OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', :search, '%'))
      OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
      OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%'))
  )
  AND u.id NOT IN (
      SELECT epa.employee_id 
      FROM employee_property_assignments epa 
      WHERE epa.status = :activeStatus
  )
ORDER BY u.created_at DESC
OFFSET :offset LIMIT :limit;
-- Explanation: Finds Users with role=EMPLOYEE who do NOT have any ACTIVE record in employee_property_assignments.
```

---

## 2. Listing Assigned Employees

### Search Employees by Property (For Manager)
```sql
-- EmployeePropertyAssignmentRepository.findByPropertyIdAndStatusWithSearch
SELECT epa.*
FROM employee_property_assignments epa
JOIN epa.employee e
WHERE epa.property_id = :propertyId
  AND epa.status = :status
  AND (
      :search IS NULL
      OR LOWER(e.full_name) LIKE LOWER(CONCAT('%', :search, '%'))
      OR LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%'))
      OR LOWER(e.phone) LIKE LOWER(CONCAT('%', :search, '%'))
  )
ORDER BY epa.assigned_at DESC
OFFSET :offset LIMIT :limit;
```

---

## 3. Reassignment & Integrity

### Flush Old Assignment (Reassign)
```sql
-- Triggered by assignmentRepository.saveAndFlush(current) before saving the new one
UPDATE employee_property_assignments
SET status = 'INACTIVE'
WHERE id = :oldAssignmentId;
```

### Database Constraint: One Active Assignment Per Employee
```sql
-- DB Level Constraint (Not explicitly a query, but part of schema rules)
CREATE UNIQUE INDEX uq_epa_one_active_per_employee 
ON employee_property_assignments (employee_id) 
WHERE status = 'ACTIVE';
```

---

## 4. Employee Creation Transaction
```sql
-- 1. Insert User
INSERT INTO users (id, email, password_hash, role, status, full_name, created_at, updated_at)
VALUES (gen_random_uuid(), :email, :hash, 'EMPLOYEE', 'ACTIVE', :fullName, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 2. Insert Assignment
INSERT INTO employee_property_assignments (id, employee_id, property_id, assigned_by, assigned_at, status)
VALUES (gen_random_uuid(), :newUserId, :propertyId, :managerId, CURRENT_TIMESTAMP, 'ACTIVE');

-- 3. Insert Activity Log
INSERT INTO activity_logs (id, actor_id, action, target_type, target_id, details, created_at)
VALUES (gen_random_uuid(), :managerId, 'EMPLOYEE_CREATED', 'User', :newUserId, '...', CURRENT_TIMESTAMP);
```
