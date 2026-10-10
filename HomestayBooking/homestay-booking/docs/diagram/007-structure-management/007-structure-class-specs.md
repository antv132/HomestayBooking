# 007 - Structure Management: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Entities

### Floor Class
Represents a physical level in a property.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. Constraints: `floor_number` must be > 0 and UNIQUE within the same `property_id`.) |

---

## 2. Controllers

### StructureTreeController Class
Handles fetching the property structure tree.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getPropertyStructure | Takes `UUID propertyId`. Returns the full tree: Property -> Floors -> Rooms. Uses `@PropertyAccessValidator(propertyId)`: Admins bypass this and can view any property tree. Managers must have an `ACTIVE` assignment to the requested `propertyId`. Returns 200 OK. |

### FloorController Class
Handles CRUD operations for the `Floor` entity (Manager Only).
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | createFloor | Takes `UUID propertyId, CreateFloorRequest`. Creates a new floor in the given property. Admins are strictly forbidden (403). Returns 201 Created. |
| 02 | updateFloor | Takes `UUID floorId, UpdateFloorRequest`. Updates floor details (e.g. floor number, name). Returns 200 OK. |
| 03 | deleteFloor | Takes `UUID floorId`. Deletes a floor if empty. Returns 204 No Content or 200 OK. |

---

## 3. Services

### StructureTreeService Class
Constructs the full property hierarchy tree.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getTree | Instead of making N+1 queries, executes a single `LEFT JOIN` query between `floors` and `rooms` to fetch all data for a property, then groups them in application memory into the nested `StructureTreeResponse` DTO. Returns `StructureTreeResponse`. |

### FloorService Class
Contains business logic for managing floors.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | createFloor | Validates that `floor_number` is not already used in the given `property_id`. Saves the new floor. Returns saved floor details. |
| 02 | deleteFloor | Must verify that the floor contains exactly 0 child rooms before allowing deletion. If rooms exist, throws exception (rooms must be reassigned or deleted via FR-08 first). Deletes the floor. Returns void. |
