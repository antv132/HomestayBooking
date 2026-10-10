# Research: FR-03 Room Discovery

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Docs**: `docs/Specification_v2.md`, `docs/api-spec-by-screen.md`, frontend `roomsApi.ts`, `publicApi.ts`

## 1. Public API Security

**Decision**: Mark discovery endpoints `permitAll` in `SecurityConfig`; optional JWT ignored for read. Customer/Guest same access.

**Rationale**: SCR-07/08/09 Role GUEST, CUSTOMER; no write operations in FR-03.

**Alternatives considered**: Require auth for search — rejected (Guest must browse).

## 2. Room List Filters (extended beyond api-spec)

**Decision**: `GET /api/v1/rooms` accepts: `page`, `size`, `sort`, `propertyId`, `propertyIds`, `location`, `search`, `roomType`, `minPrice`, `maxPrice`, `capacity`, `checkIn`, `checkOut`, `status` (default bookable set).

**Rationale**: Frontend `useRoomSearch.ts` + `FetchRoomsParams` already define filters; api-spec only lists 4 params — extend contract.

**Alternatives considered**: Minimal api-spec only — rejected (breaks existing UI).

## 3. Availability Endpoints (dual)

**Decision**: Support **both**:
- `GET /api/v1/rooms/{id}/availability?month=&year=` → `{ bookedDates: [] }` (SCR-09 api-spec)
- `GET /api/v1/rooms/{id}/availability?checkIn=&checkOut=` → `{ available, bookedRanges }` (frontend `checkRoomAvailability`)
- `GET /api/v1/rooms/{id}/calendar` → `{ roomStatus, bookedRanges }` (frontend `fetchRoomCalendar`)

**Rationale**: SCR-09 + existing `RoomDetailPage` mini calendar + `RoomAvailabilityCalendar` component.

**Alternatives considered**: Single endpoint — rejected (different response shapes in use).

## 4. Availability Logic

**Decision**: Booked = overlapping bookings with status in `{ PENDING_DEPOSIT, CONFIRMED, RESERVED, OCCUPIED }` for date range. Room status `MAINTENANCE`, `OUT_OF_SERVICE` excluded from search results entirely.

**Rationale**: spec edge cases; FR-04 booking states.

**Alternatives considered**: Only CONFIRMED — rejected (Pending Deposit holds inventory per FR-04).

## 5. Amenities Storage

**Decision**: `rooms.amenities` as `JSONB` or `TEXT[]` storing Vietnamese tag strings (WiFi, Điều hòa, …) matching frontend `AMENITY_ICONS`.

**Rationale**: spec Assumptions; no separate Amenity entity in §5; FR-08 manager sets on create.

**Alternatives considered**: Normalized amenity table — deferred (YAGNI for discovery read).

## 6. Pricing Display

**Decision**: `PricingService.resolve(roomId, checkIn, checkOut)` — highest-priority active PricingRule for property/roomType in range; fallback `Room.pricePerNight`. List cards show nightly price for search range or base price.

**Rationale**: spec Assumptions + PricingRule entity §5.

**Alternatives considered**: Static pricePerNight only — rejected (spec mentions dynamic pricing).

## 7. Featured Rooms & Properties

**Decision**:
- Featured rooms: ACTIVE property + status AVAILABLE + has primary image; order by `averageRating DESC`, `totalReviews DESC`, limit param (default 8).
- Featured properties: ACTIVE; include `roomCount`, `availableRoomCount`, optional cover image URL.

**Rationale**: LandingPage.tsx + api-spec `GET /properties/featured`; frontend `FeaturedRoom`/`FeaturedProperty` types.

## 8. Search Suggestions

**Decision**: `GET /api/v1/public/search-suggestions?q=` — ILIKE match on `Property.name`, `Property.address`; return `{ type: 'property'|'location', label }`; min query length 2; limit 8; debounce client-side 300ms.

**Rationale**: spec US-3; frontend `SearchSuggestion` type.

**Alternatives considered**: Elasticsearch — overkill for MVP.

## 9. Platform Stats

**Decision**: `GET /api/v1/public/stats` returns `{ totalProperties, totalRooms, totalAvailableRooms, averageRating, totalReviews }` — aggregate SQL counts on ACTIVE data.

**Rationale**: frontend `PlatformStats` interface + Guest permission.

## 10. Promotions

**Decision**: `GET /api/v1/promotions/active` per api-spec SCR-01; map to frontend `Promotion` shape (title, subtitle, ctaUrl, colorTheme).

**Rationale**: api-spec path differs from frontend `/api/public/promotions` — standardize to api-spec.

## 11. Date Validation

**Decision**: Server rejects `checkOut <= checkIn`, `checkIn` before today (local property timezone Asia/Ho_Chi_Minh); return 400 with field errors per §7.

**Rationale**: FR-010; client also validates in `useRoomSearch`.

## 12. Pagination & Sort

**Decision**: Spring `Pageable`; sort whitelist: `pricePerNight,asc|desc`, `createdAt,desc` (maps frontend `price-asc`, `price-desc`, `newest`).

**Rationale**: api-spec §1 pagination envelope; frontend `sortToApi()`.

## 13. Reviews on Detail Page

**Decision**: Include review summary (`averageRating`, `totalReviews`) in list/detail; paginated `GET /api/v1/rooms/{id}/reviews` optional P2 — frontend already calls it; include in contract as read-only discovery enrichment (not write review — out of scope).

**Rationale**: `RoomDetailPage.tsx` displays reviews; read-only OK for discovery.
