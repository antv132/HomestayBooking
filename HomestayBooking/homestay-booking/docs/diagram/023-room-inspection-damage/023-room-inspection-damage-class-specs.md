# 023 - Room Inspection & Damage: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Domain Overview
The Room Inspection module manages the checklist employees must fill out when checking a room (typically after a customer checks out). If the room is fine, the inspection is `PASSED`. If items are missing or broken, it is `FAILED_WITH_DAMAGE`. This ties closely into the Damage Report system.

---

## 2. Dynamic Checklists (Entities)

### ChecklistItemDefinition Class
Defines the questions (e.g., "TV Remote present?"). They can be global (`property_id IS NULL`) or property-specific.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. Stores the dynamic questions for checklists.) |

### InspectionChecklistAnswer Class
Stores the boolean result for a specific item during a specific inspection.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. Contains `passed` = true/false for a specific `RoomInspection`.) |

---

## 3. Controllers

### ManagerInspectionV1Controller Class
Handles inspection management for Managers.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getInspections | GET `/`. Managers view all inspections for their properties. Returns 200 OK. |
| 02 | assignInspector | POST `/{id}/assign`. Managers proactively assign an employee to an inspection. Changes status from `PENDING` to `IN_PROGRESS`. Returns 200 OK. |

### EmployeeInspectionController Class
Handles inspection execution for Employees.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getActiveChecklistItems | GET `/checklist-items`. Returns the definitions so the frontend can dynamically render the form. Returns 200 OK. |
| 02 | passInspection | POST `/{id}/pass`. Employee marks inspection as passed with checklist answers. Returns 200 OK. |
| 03 | failInspection | POST `/{id}/fail`. Employee marks inspection as failed with damage notes and checklist answers. Returns 200 OK. |

---

## 4. Services

### RoomInspectionManagerService Class
Handles logic for Manager interactions.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | findForManagerBoard | Fetches all inspections for the manager's properties. Returns paginated list. |
| 02 | assignInspector | Assigns an employee to an inspection and updates status to `IN_PROGRESS`. Returns updated inspection. |

### EmployeeInspectionService Class
Handles logic for Employee checklist execution.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | listActiveChecklistItems | Returns the definitions so the mobile/web frontend can dynamically render the form. Returns list of items. |
| 02 | claimIfUnassigned | Auto-Claiming: If an employee tries to `pass` or `fail` an unassigned inspection, the system automatically claims it for them. Returns void. |
| 03 | pass | Validates and saves checklist answers. Deletes any old answers for idempotency. Sets status to `PASSED`. Appends checklist results to the `note` field. Returns updated inspection. |
| 04 | fail | Requires a manual `note` describing the damage. Saves checklist answers. Sets status to `FAILED_WITH_DAMAGE`. Returns updated inspection. |
