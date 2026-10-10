# 006 - Property Management: Database Queries

This document outlines the queries used for managing Properties and their assignments.

## 1. Property CRUD

### Insert Property
```sql
INSERT INTO properties (
    id, name, address, description, status, created_at, updated_at
) VALUES (
    :id, :name, :address, :description, 'INACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
```

### Admin Search Properties (with current Manager)
Uses `ILIKE` or `pg_trgm` depending on database capabilities.
```sql
SELECT 
    p.id, p.name, p.address, p.status, 
    u.id AS manager_id, u.full_name AS manager_name
FROM properties p
LEFT JOIN manager_property_assignments mpa 
    ON p.id = mpa.property_id AND mpa.status = 'ACTIVE'
LEFT JOIN users u 
    ON mpa.manager_id = u.id
WHERE (:keyword IS NULL OR p.name ILIKE :keyword OR p.address ILIKE :keyword)
  AND (:status IS NULL OR p.status = :status)
ORDER BY p.created_at DESC
OFFSET :offset LIMIT :limit;
```

## 2. Manager Assignments

*(Runs inside a transaction during `POST /api/admin/properties/{id}/managers`)*

### Deactivate Current Assignment
```sql
UPDATE manager_property_assignments
SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP
WHERE property_id = :propertyId AND status = 'ACTIVE';
```

### Insert New Assignment
```sql
INSERT INTO manager_property_assignments (
    id, manager_id, property_id, assigned_by, assigned_at, status, created_at, updated_at
) VALUES (
    :id, :managerId, :propertyId, :adminId, CURRENT_TIMESTAMP, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
```

## 3. Validation & Access Control

### Check Property Activation Eligibility
```sql
SELECT COUNT(1) 
FROM manager_property_assignments 
WHERE property_id = :propertyId AND status = 'ACTIVE';
-- Must return 1 before allowing property status -> ACTIVE
```

### Check Manager Access (PropertyAccessValidator)
```sql
SELECT CASE WHEN EXISTS (
    SELECT 1 FROM manager_property_assignments 
    WHERE property_id = :propertyId 
      AND manager_id = :managerId 
      AND status = 'ACTIVE'
) THEN 1 ELSE 0 END;
```

### Fetch Properties for Logged-in Manager
```sql
SELECT p.* 
FROM properties p
INNER JOIN manager_property_assignments mpa 
    ON p.id = mpa.property_id
WHERE mpa.manager_id = :managerId 
  AND mpa.status = 'ACTIVE'
ORDER BY p.name ASC;
```
