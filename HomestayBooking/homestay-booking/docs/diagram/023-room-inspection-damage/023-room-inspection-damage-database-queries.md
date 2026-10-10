# 023 - Room Inspection & Damage: Database Queries

## 1. Dynamic Checklist Definitions

### Fetch Active Checklist Definitions
```sql
-- ChecklistItemDefinitionRepository.findByIsActiveTrueAndPropertyIsNullOrderBySortOrderAsc
SELECT *
FROM checklist_item_definitions
WHERE is_active = TRUE
  AND property_id IS NULL
ORDER BY sort_order ASC;
```

## 2. Managing Checklist Answers

### Delete Old Answers (Idempotency before Save)
```sql
-- InspectionChecklistAnswerRepository.deleteByInspectionId
DELETE FROM inspection_checklist_answers
WHERE inspection_id = :inspectionId;
```

### Save Checklist Answers
```sql
-- Managed by Spring Data JPA
INSERT INTO inspection_checklist_answers (
    id, inspection_id, checklist_item_id, passed, created_at, updated_at
) VALUES (
    gen_random_uuid(), :inspectionId, :itemId, :passed, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
```

## 3. Inspection Listing & Scoping

### List for Manager Dashboard
```sql
-- RoomInspectionRepository.findForManagerBoard
SELECT ri.*
FROM room_inspections ri
JOIN rooms r ON ri.room_id = r.id
JOIN bookings b ON ri.booking_id = b.id
LEFT JOIN users e ON ri.assigned_employee_id = e.id
WHERE r.property_id = :propertyId
  AND (:status IS NULL OR ri.status = :status)
  AND (
      :search IS NULL
      OR LOWER(r.room_number) LIKE LOWER(CONCAT('%', :search, '%'))
      OR LOWER(e.full_name) LIKE LOWER(CONCAT('%', :search, '%'))
      OR LOWER(b.customer_name) LIKE LOWER(CONCAT('%', :search, '%'))
  )
ORDER BY ri.created_at DESC
OFFSET :offset LIMIT :limit;
```

### List for Employee App
```sql
-- RoomInspectionRepository.findForEmployee
SELECT ri.*
FROM room_inspections ri
JOIN rooms r ON ri.room_id = r.id
WHERE r.property_id IN (:propertyIds)
  AND ri.status IN (:statuses)
  AND (ri.assigned_employee_id = :employeeId OR ri.assigned_employee_id IS NULL)
ORDER BY ri.created_at ASC
OFFSET :offset LIMIT :limit;
-- Note: Includes unassigned inspections that the employee can "claim".
```

## 4. State Transitions (Employee Actions)

### Pass Inspection
```sql
UPDATE room_inspections
SET status = 'PASSED',
    inspected_by_id = :employeeId,
    inspected_at = CURRENT_TIMESTAMP,
    note = :generatedNote,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :inspectionId;
```

### Fail Inspection
```sql
UPDATE room_inspections
SET status = 'FAILED_WITH_DAMAGE',
    inspected_by_id = :employeeId,
    inspected_at = CURRENT_TIMESTAMP,
    note = :manualNoteAndChecklistResults,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :inspectionId;
```
