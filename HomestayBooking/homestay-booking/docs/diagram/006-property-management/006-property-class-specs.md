# 006 - Property Management: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Entities

### Property Class
Represents a physical branch (homestay/resort).
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. Default Status: `INACTIVE` upon creation. Constraints: Cannot be transitioned to `ACTIVE` unless there is exactly one `ACTIVE` `ManagerPropertyAssignment`.) |

### ManagerPropertyAssignment Class
Represents the many-to-many junction mapping Users (Managers) to Properties.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. Only one assignment can be `ACTIVE` per property at any given time. Maintains a historical ledger: when a manager is replaced, their assignment is marked `INACTIVE` instead of being deleted.) |

---

## 2. Controllers

### AdminPropertyController Class
Handles property management operations for Admins.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | createProperty | Takes `CreatePropertyRequest`. Creates a new property. Defaults to INACTIVE. Returns 201 Created. |
| 02 | updateProperty | Takes `UUID id, UpdatePropertyRequest`. Updates name, address, etc. Returns 200 OK. |
| 03 | setPropertyStatus | Takes `UUID id, String status`. Strictly validates Manager presence before activating. Returns 200 OK. |
| 04 | assignManager | Takes `UUID propertyId, AssignManagerRequest`. Enforces role checks (`MANAGER`) and handles atomic deactivation of previous assignments. Returns 200 OK. |
| 05 | getProperties | Takes `FilterRequest`. Retrieves a paginated list of properties for the Admin dashboard. Returns 200 OK. |

### ManagerPropertyController Class
Handles property viewing for Managers.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getMyProperties | Returns only properties currently assigned to the logged-in Manager. Returns 200 OK. |
| 02 | getPropertyDetail | Takes `UUID propertyId`. Enforces `@PropertyAccessValidator` to ensure the Manager is authorized to view this specific property. Returns aggregate stats (e.g. total rooms, available rooms). Returns 200 OK. |

---

## 3. Services

### PropertyService Class
Handles CRUD for properties.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | setStatus | Contains the critical business logic verifying `ManagerPropertyAssignmentRepository.countActiveAssignments(propertyId) == 1` before allowing an `ACTIVE` transition. Updates and saves property status. Returns updated Property details. |

### ManagerAssignmentService Class
Handles the delegation of properties.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | assignManager | Runs in a `@Transactional` block. 1) Validates the `managerId` exists, is active, and possesses the `MANAGER` role. 2) Executes `deactivateCurrentAssignment(propertyId)`. 3) Inserts the new `ManagerPropertyAssignment` as `ACTIVE`. Returns void or assignment summary. |

---

## 4. Security & Validation

### PropertyAccessValidator Class
A security abstraction (e.g., Spring Security `@PreAuthorize` or custom interceptor) applied to all `Manager...Controller` endpoints that take a `propertyId`.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | validateAccess | Queries the assignment table to confirm the JWT's `userId` has an `ACTIVE` assignment to the requested `propertyId`. Returns true if authorized, throws 403 Forbidden if not. |
