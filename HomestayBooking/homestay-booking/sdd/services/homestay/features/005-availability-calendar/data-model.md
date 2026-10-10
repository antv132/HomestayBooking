# Data Model: FR-05 Availability Calendar

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: `docs/Specification_v2.md` §5 Room, Booking, HousekeepingTask

## Scope

FR-05 adds **room_status_blocks**, **calendar computation services**, and extends read APIs. Does not own booking or housekeeping writes (FR-04, FR-21).

## ERD

```text
Room 1──* RoomStatusBlock
Room 1──* Booking (read — calendar source)
Room 1──* HousekeepingTask (read — calendar source)
Room.status (operational enum — current snapshot)
CalendarDayStatus (derived, not stored)
```

## New Table: room_status_blocks

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| room_id | UUID | FK rooms NOT NULL | |
| property_id | UUID | FK properties NOT NULL | Denormalized for manager queries |
| status | VARCHAR(32) | NOT NULL | MAINTENANCE, OUT_OF_SERVICE |
| start_date | DATE | NOT NULL | |
| end_date | DATE | NOT NULL, >= start_date | |
| reason | TEXT | nullable | |
| created_by | UUID | FK users | Manager/Admin |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |
| active | BOOLEAN | DEFAULT true | Soft cancel block |

**Indexes**: `(room_id, start_date, end_date)`, `(property_id, active)`.

**Validation on insert**: No overlap with active bookings (FR-04 blocking statuses) for `[start_date, end_date)`.

## Room.status (existing — §5)

Operational enum (8 values):

| Value | Calendar role |
|-------|-----------------|
| AVAILABLE | Default bookable when no other source |
| PENDING_DEPOSIT | Usually from booking; room-level rare |
| RESERVED | Booking-driven |
| OCCUPIED | Booking-driven |
| PENDING_CLEANING | HK / post-checkout |
| CLEANING_IN_PROGRESS | HK in progress |
| MAINTENANCE | Manual block or room snapshot |
| OUT_OF_SERVICE | Manual block or room snapshot |

`Room.status` reflects **current** operational state; calendar uses blocks + bookings for **future/past days**.

## CalendarDayStatus (derived DTO)

| Field | Type | Description |
|-------|------|-------------|
| date | LocalDate | Calendar day |
| status | RoomCalendarStatus | One of 8 display statuses |
| bookable | boolean | true only if status == AVAILABLE (and room/property ACTIVE) |
| bookingId | UUID | nullable — manager view only |
| blockId | UUID | nullable — manager view only |

### RoomCalendarStatus enum (display)

Maps 1:1 to spec labels: `AVAILABLE`, `PENDING_DEPOSIT`, `RESERVED`, `OCCUPIED`, `PENDING_CLEANING`, `CLEANING_IN_PROGRESS`, `MAINTENANCE`, `OUT_OF_SERVICE`.

## Status Resolution Algorithm (per day D)

```text
sources = []

FOR each active booking B where D in [B.checkIn, B.checkOut):
  map B.status → calendar status → add to sources

FOR each active room_status_block R where D in [R.startDate, R.endDate]:
  add R.status to sources

IF D == today(propertyTz) OR room in post-checkout window:
  map Room.status HK states → add to sources

RETURN CalendarStatusResolver.highestPriority(sources) OR AVAILABLE
```

### Priority (high → low)

1. OCCUPIED  
2. PENDING_DEPOSIT  
3. RESERVED  
4. CLEANING_IN_PROGRESS  
5. PENDING_CLEANING  
6. MAINTENANCE  
7. OUT_OF_SERVICE  
8. AVAILABLE  

## Booking Read Rules (overlap)

Include bookings with status in:

`PENDING_DEPOSIT`, `CONFIRMED`, `CHECKED_IN`, `PENDING_INSPECTION`, `PENDING_DAMAGE_PAYMENT`

Exclude: `CANCELLED`, `CHECKED_OUT`, `NO_SHOW`.

## Housekeeping Read Rules

- Open task (status ≠ COMPLETED, ≠ CANCELLED) for room → contributes PENDING_CLEANING or CLEANING_IN_PROGRESS based on task.status.
- Gate manual `AVAILABLE`: reject if any open HK task for room.

## API DTOs

### RoomCalendarResponse (public month)

```json
{
  "roomId": "uuid",
  "month": 6,
  "year": 2026,
  "roomStatus": "AVAILABLE",
  "days": [
    { "date": "2026-06-01", "status": "AVAILABLE", "bookable": true }
  ],
  "bookedDates": ["2026-06-15"]
}
```

### ManagerRoomCalendarResponse

Extends with `days[].bookingId`, `days[].blockId`, `days[].guestHint` (optional masked).

### UpdateRoomStatusRequest (SCR-33)

```json
{
  "status": "MAINTENANCE",
  "startDate": "2026-07-01",
  "endDate": "2026-07-05",
  "reason": "Plumbing work"
}
```

### RoomStatusBlockResponse

```json
{
  "id": "uuid",
  "status": "MAINTENANCE",
  "startDate": "2026-07-01",
  "endDate": "2026-07-05",
  "reason": "..."
}
```

## Flyway

```text
V030__room_status_blocks.sql
```

## ActivityLog

- `ROOM_STATUS_BLOCK_CREATED`, `ROOM_STATUS_BLOCK_DEACTIVATED`, `ROOM_STATUS_MANUAL_UPDATE`

## Integration Points

| Feature | Integration |
|---------|-------------|
| FR-03 | `GET /rooms/{id}/availability` delegates to CalendarStatusService |
| FR-04 | Booking create/cancel triggers no calendar cache — next read reflects |
| FR-21 | HK task status updates Room.status → visible on calendar |
| FR-15 | Future: subscribe calendar room channel for instant invalidation |
