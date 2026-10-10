# 013 - Maintenance Management: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Entities

### MaintenanceTicket Class
Represents a maintenance request or issue reported in a room.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. Contains `customer_id`, `room_id`, `booking_id`, `assigned_employee_id`, `title`, `description`, `photo_urls` (JSON array), `status`, `resolution_note`, `work_note`, `deleted_at`. Status enum: `OPEN`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`.) |

---

## 2. Controllers

### CustomerMaintenanceController Class
Handles maintenance operations for Customers (SCR-31).
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | createTicket | POST `/`. Create a ticket. Requires an active booking (`CONFIRMED` or `CHECKED_IN`). Returns 201 Created. |
| 02 | listMyTickets | GET `/`. List my tickets (paged, optional status filter). Returns 200 OK. |
| 03 | updateTicket | PUT `/{id}`. Update ticket content (title, desc, photos) only if status is `OPEN`. Returns 200 OK. |
| 04 | deleteTicket | DELETE `/{id}`. Soft delete ticket only if status is `OPEN`. Returns 204 No Content. |

### ManagerMaintenanceV1Controller Class
Handles maintenance operations for Managers (SCR-41).
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | listTickets | GET `/`. List tasks scoped to a specific `propertyId`. Returns 200 OK. |
| 02 | assignTicket | POST `/{id}/assign`. Assign a ticket to an Employee. Validates that the employee is active in the same property. Changes state to `ASSIGNED`. Returns 200 OK. |
| 03 | closeTicket | PATCH `/{id}/status`. Manager Verify & Close. Can only transition from `RESOLVED` to `CLOSED`. Requires `resolutionNote`. Returns 200 OK. |

### EmployeeMaintenanceController Class
Handles maintenance operations for Employees (SCR-61).
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | listAssignedTickets | GET `/`. List tasks assigned specifically to this employee. Returns 200 OK. |
| 02 | updateTicketStatus | PATCH `/{id}/status`. Employee updates progress. Allowed transitions: `ASSIGNED` → `IN_PROGRESS` → `RESOLVED`. Requires a work note when resolving. Returns 200 OK. |

---

## 3. Services & State Machine

### MaintenanceService Class (Conceptual grouping of logic)
Handles the core state transitions and scoping for maintenance tickets.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | enforceScope | Customer: view/edit tickets where `customer_id == currentUser.id`. Manager: operations scoped to `propertyId` via `ReportPropertyScopeValidator`. Employee: view/edit tickets where `assigned_employee_id == currentUser.id`. Throws exceptions on violation. |
| 02 | logAndNotify | Ticket creation logs `TICKET_CREATED`. Manager assigning logs `TICKET_ASSIGNED` and sends a notification to Customer. Employee resolving logs `TICKET_STATUS_CHANGED` and sends notification to Customer. Manager closing logs `TICKET_CLOSED` and sends final notification to Customer. Returns void. |
