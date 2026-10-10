# Data Model: FR-03 Room Discovery

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: `docs/Specification_v2.md` §5 Property, Room, RoomImage, Booking, PricingRule

## Scope

FR-03 is **read-only** for discovery. No new core tables required if FR-06/08 migrations exist. Optional: `promotions` table for SCR-01 banners; `rooms.amenities` column if not in FR-08 schema.

## ERD (discovery read paths)

```text
Property 1──* Floor 1──* Room 1──* RoomImage
Room 1──* Booking (overlap queries for availability)
Property 1──* PricingRule
Promotion (standalone, ACTIVE flag)
```

## Entities Used

### Property (read filter: status = ACTIVE)

| Column | FR-03 Use |
|--------|-----------|
| id, name, address, description, status | List filter, detail context, search suggestions |

### Floor

| Column | FR-03 Use |
|--------|-----------|
| id, propertyId, floorNumber | Detail display (optional breadcrumb) |

### Room (discovery filter)

| Column | FR-03 Use |
|--------|-----------|
| id, propertyId, floorId, roomNumber, roomType | List, detail, filters |
| area, capacity, pricePerNight | Cards, detail |
| status | Filter: exclude MAINTENANCE, OUT_OF_SERVICE from guest search |
| description | Detail SCR-08 |
| amenities | JSONB/TEXT[] — detail amenities list |

**Guest-visible statuses in search (no dates)**: primarily `AVAILABLE` (badge may show others on detail only).

### RoomImage

| Column | FR-03 Use |
|--------|-----------|
| roomId, imageUrl, isPrimary, sortOrder | Gallery; list card uses primary |

### Booking (read-only overlap)

| Column | FR-03 Use |
|--------|-----------|
| roomId, checkInDate, checkOutDate, status | Availability calendar + date filter |

**Blocking statuses for availability**: `PENDING_DEPOSIT`, `CONFIRMED`, `RESERVED`, `OCCUPIED`.

### PricingRule (optional resolution)

| Column | FR-03 Use |
|--------|-----------|
| propertyId, roomTypeId, startDate, endDate, pricePerNight, priority | Display price for search date range |

### Promotion (SCR-01)

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| title, subtitle, description | String | Banner content |
| ctaText, ctaUrl | String | CTA |
| colorTheme | String | red/blue/green |
| isActive | Boolean | Only ACTIVE shown |
| sortOrder | Integer | Display order |
| imageUrl | String | Optional banner image |

## Query Rules

### Room search (default)

```text
WHERE property.status = 'ACTIVE'
  AND room.status NOT IN ('MAINTENANCE', 'OUT_OF_SERVICE')
  AND (optional filters: propertyId, roomType, price range, capacity, location ILIKE)
  AND (if checkIn/checkOut: no overlapping blocking booking AND capacity >= guests)
```

### Availability month (SCR-09)

```text
For each day in month: mark booked if ∃ booking overlap with blocking status
Return: bookedDates[] as ISO date strings
```

## DTOs (API response shapes)

### RoomListItem (paginated content[])

```json
{
  "id": "uuid",
  "roomNumber": "Villa 01",
  "roomType": "Villa",
  "pricePerNight": 2500000,
  "capacity": 4,
  "area": 65.5,
  "status": "AVAILABLE",
  "primaryImageUrl": "https://...",
  "propertyId": "uuid",
  "propertyName": "Sunset Resort",
  "floorNumber": 1,
  "averageRating": 4.5,
  "totalReviews": 12
}
```

### RoomDetail

```json
{
  "id": "uuid",
  "roomNumber": "Villa 01",
  "roomType": "Villa",
  "pricePerNight": 2500000,
  "capacity": 4,
  "area": 65.5,
  "description": "...",
  "status": "AVAILABLE",
  "propertyId": "uuid",
  "propertyName": "Sunset Resort",
  "propertyAddress": "Đà Nẵng, ...",
  "images": [{ "url": "...", "isPrimary": true, "sortOrder": 0 }],
  "amenities": ["WiFi", "Điều hòa"],
  "averageRating": 4.5,
  "totalReviews": 12
}
```

### RoomAvailability (month mode)

```json
{ "bookedDates": ["2026-06-15", "2026-06-16"] }
```

### RoomAvailability (range mode)

```json
{
  "available": true,
  "bookedRanges": [{ "checkIn": "2026-06-15", "checkOut": "2026-06-17", "bookingStatus": "CONFIRMED" }]
}
```

## Validation Rules

| Input | Rule | Error |
|-------|------|-------|
| checkIn, checkOut | checkOut > checkIn | Validation failed |
| checkIn | >= today (VN timezone) | Invalid date |
| guests / capacity | guests <= room.capacity when filtering | (exclude room, no error) |
| page, size | size max 50 | Validation failed |

## Indexes (recommended)

- `rooms(property_id, status)`
- `rooms(price_per_night)`
- `bookings(room_id, check_in_date, check_out_date)` + status
- `properties(status)` + GIN/trgm on `name`, `address` for suggestions

## Out of Scope (data writes)

- Create/update Room, Property (FR-06, FR-08)
- Create Booking (FR-04)
- Manager status calendar update (FR-05)
