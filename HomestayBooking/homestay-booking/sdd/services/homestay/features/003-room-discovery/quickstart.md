# Quickstart: FR-03 Room Discovery

**Feature**: `specs/003-room-discovery` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: Seed data — ít nhất 1 Property ACTIVE, vài Room AVAILABLE với RoomImage (Flyway seed hoặc FR-08).

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server 2022+
- Backend running with public endpoints `permitAll`

## Environment

Same as FR-01 quickstart. No additional secrets.

```bash
# Frontend/.env — optional
VITE_API_URL=
```

Vite proxy: `/api/v1` → `http://localhost:8080`

## Run

```bash
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

## Screen → Route → API

| Screen | Route | API |
|--------|-------|-----|
| SCR-01 Landing | `/` | `GET /promotions/active`, `/properties/featured`, `/rooms/featured`, `/public/stats` |
| SCR-07 Search/Listing | `/search`, `/rooms` | `GET /rooms?...filters` |
| SCR-08 Room Detail | `/rooms/:id` | `GET /rooms/{id}`, `/rooms/{id}/calendar` |
| SCR-09 Calendar | `/rooms/:id/calendar` | `GET /rooms/{id}/availability?month&year` |
| Search suggestions | Hero bar | `GET /public/search-suggestions?q=` |

## curl smoke tests

```bash
BASE=http://localhost:8080/api/v1

# List rooms (SCR-07)
curl -s "$BASE/rooms?page=0&size=12&sort=pricePerNight,asc" | jq

# Search with dates + guests
curl -s "$BASE/rooms?checkIn=2026-07-10&checkOut=2026-07-13&capacity=2&location=Da%20Nang" | jq

# Invalid dates (expect 400)
curl -s "$BASE/rooms?checkIn=2026-07-13&checkOut=2026-07-10" | jq

# Room detail (SCR-08)
ROOM_ID="<uuid>"
curl -s "$BASE/rooms/$ROOM_ID" | jq

# Availability month (SCR-09)
curl -s "$BASE/rooms/$ROOM_ID/availability?month=7&year=2026" | jq

# Range availability
curl -s "$BASE/rooms/$ROOM_ID/availability?checkIn=2026-07-10&checkOut=2026-07-13" | jq

# Calendar compat
curl -s "$BASE/rooms/$ROOM_ID/calendar" | jq

# Landing content (SCR-01)
curl -s "$BASE/rooms/featured?limit=8" | jq
curl -s "$BASE/properties/featured?limit=6" | jq
curl -s "$BASE/public/stats" | jq
curl -s "$BASE/promotions/active" | jq

# Search suggestions
curl -s "$BASE/public/search-suggestions?q=Da" | jq

# Price stats for filter slider
curl -s "$BASE/rooms/price-stats" | jq
```

## Frontend verification

1. **Landing** (`/`) — hero search, featured rooms/properties, stats, promotion banners.
2. **Search** (`/search?location=...&checkIn=...`) — filters, pagination, empty state.
3. **Room detail** (`/rooms/:id`) — gallery, amenities, mini calendar, Book Now → login if guest.
4. **Calendar** (`/rooms/:id/calendar`) — month navigation, booked vs available days.
5. **Suggestions** — type in hero search, pick suggestion → navigates to filtered search.

## Tests

```bash
cd backend && ./mvnw test -Dtest=AvailabilityServiceTest,RoomDiscoveryControllerIT
cd frontend && npm run test -- useRoomSearch
```

## Out of scope

- Booking checkout (FR-04 / SCR-16)
- Manager room CRUD (FR-08)
- Write reviews (separate FR)

## Seed data tip

If no FR-08 yet, apply dev seed migration with 2 properties, 10+ rooms, sample bookings for calendar testing.
