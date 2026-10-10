# 021 - Housekeeping Management: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Entities & States

### HousekeepingTask Class
Represents a room cleaning task.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. States (`Status`): `PENDING` (created, awaiting execution), `IN_PROGRESS` (employee started cleaning), `COMPLETED` (cleaning finished), `CANCELLED` (manager cancelled). When a task is created, the room status becomes `PENDING_CLEANING`. When started -> `CLEANING_IN_PROGRESS`. When finished -> `AVAILABLE`.) |

---

## 2. Controllers

### ManagerHousekeepingV1Controller Class
Handles housekeeping CRUD and scheduling for Managers.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | createTask | Manually queues a cleaning task for a room. Returns 201 Created. |
| 02 | assignTask | Links a task to a specific `EMPLOYEE` assigned to that property. Returns 200 OK. |
| 03 | getSchedule | Builds a matrix/Kanban-like view grouping tasks by assigned employees and unassigned tasks for a specific date. Returns 200 OK. |

### EmployeeHousekeepingController Class
Handles task execution for Employees.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | startTask | Allows employees to start their assigned task. Returns 200 OK. |
| 02 | finishTask | Allows employees to finish their assigned task. Returns 200 OK. |

### AdminHousekeepingController Class
Read-only overview for Admins.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getOverview | Provides read-only overview of housekeeping tasks. Returns 200 OK. |

---

## 3. Services

### HousekeepingTaskService Class
Handles core logic for Managers.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | onBookingCheckedOut | Auto-creation hook: Ensures a task is automatically generated when a customer checks out. It is idempotent (checks if a task already exists for the booking). Returns void. |
| 02 | createTaskForManager | Allows managers to manually queue a cleaning task for a room. Returns saved task. |
| 03 | assignTaskForManager | Links a task to a specific `EMPLOYEE` assigned to that property. Returns updated task. |
| 04 | getScheduleForManager | Builds a matrix/Kanban-like view grouping tasks by assigned employees and unassigned tasks for a specific date. Returns schedule details. |

### EmployeeHousekeepingService Class
Handles task execution for Employees.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | start | Action Execution: Allows employees to `start` their assigned tasks. Calls `notifyManagers` to push a notification (`HOUSEKEEPING_TASK_UPDATED`) to all active Managers of that property. Returns updated task. |
| 02 | finish | Action Execution: Allows employees to `finish` their assigned tasks. Calls `notifyManagers` to push a notification (`HOUSEKEEPING_TASK_UPDATED`) to all active Managers of that property. Returns updated task. |
| 03 | notifyManagers | Pushes a notification to all active Managers of the property. Returns void. |
