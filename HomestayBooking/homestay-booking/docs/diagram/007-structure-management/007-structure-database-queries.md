# 007 - Structure Management: Database Queries

## 1. Structure Tree Retrieval

### Fetch Full Property Tree (Floors + Rooms)
Executes a `LEFT JOIN` to fetch all floors and their associated rooms in one go. Sorted logically for the UI.
```sql
SELECT 
    f.id AS floor_id, f.floor_number, f.description, 
    r.id AS room_id, r.room_number, r.room_type, r.status AS room_status
FROM floors f
LEFT JOIN rooms r ON f.id = r.floor_id
WHERE f.property_id = :propertyId
ORDER BY f.floor_number ASC, r.room_number ASC;
```

## 2. Floor CRUD Validations

### Check Floor Number Uniqueness
Used during Floor Creation / Update.
```sql
SELECT CASE WHEN EXISTS (
    SELECT 1 FROM floors 
    WHERE property_id = :propertyId 
      AND floor_number = :floorNumber
      AND id != :excludeFloorId -- (Only used during Update)
) THEN 1 ELSE 0 END;
```

### Check Child Rooms Before Deletion
Ensures we don't violate business rules or foreign key constraints.
```sql
SELECT COUNT(1) 
FROM rooms 
WHERE floor_id = :floorId;
-- Must return 0 to allow deletion
```

## 3. Floor Mutators

### Insert Floor
```sql
INSERT INTO floors (
    id, property_id, floor_number, description, created_at, updated_at
) VALUES (
    :id, :propertyId, :floorNumber, :description, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
```

### Delete Floor
```sql
DELETE FROM floors WHERE id = :floorId;
```
