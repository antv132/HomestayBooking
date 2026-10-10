# 005 - Availability Calendar: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Entities & DTOs

### RoomStatusBlock Class
Represents a manual block on the calendar created by a Manager or Admin. Used specifically for `MAINTENANCE` or `OUT_OF_SERVICE`.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. Contains `active` boolean flag to easily remove a block via soft delete.) |

### CalendarDayStatus Class
Represents the computed status of a single day on the calendar.
| No | Method | Description |
|---|---|---|
| - | - | (DTO class. Does not exist in the database. Generated dynamically by the `CalendarStatusService`.) |

---

## 2. Controllers

### CalendarController Class
Handles public/guest calendar operations.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getRoomCalendar | Takes `UUID roomId, int month, int year`. Returns read-only calendar for a specific room. Hides sensitive `bookingId` or `blockId`. Returns 200 OK. |

### ManagerCalendarController Class
Handles manager and admin calendar operations.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getManagerRoomCalendar | Retrieves calendar view including IDs and guest hints for tooltip display. Returns 200 OK. |
| 02 | addStatusBlock | Takes block details. Adds a manual Maintenance/OOS block. Returns 200 OK. |
| 03 | removeStatusBlock | Takes block ID. Deactivates an existing manual block. Returns 200 OK. |

---

## 3. Services

### CalendarStatusService Class
Orchestrates the data gathering and algorithm execution to compute the calendar grid.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | computeCalendarGrid | For a requested month, queries all `Booking`s, `RoomStatusBlock`s, and open `HousekeepingTask`s overlapping that month. Iterates from day 1 to day N. For each day, gathers all sources overlapping that day and reduces them into a single status using Priority rules (e.g. `OCCUPIED` > `PENDING_DEPOSIT` > `RESERVED` > `CLEANING_IN_PROGRESS` > `PENDING_CLEANING` > `MAINTENANCE` > `OUT_OF_SERVICE` > `AVAILABLE`). Returns grid data. |

### RoomStatusBlockService Class
Handles the business logic for creating manual locks on the inventory.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | addBlock | Validates that a requested block `[start, end]` does not overlap with any active `Booking`. Adds and saves the block. Returns block details. |
| 02 | removeBlock | If a Manager attempts to manually clear a room to `AVAILABLE`, it strictly checks the `HousekeepingTaskRepository` to ensure no tasks are open. Deactivates the block. Returns void or status. |
