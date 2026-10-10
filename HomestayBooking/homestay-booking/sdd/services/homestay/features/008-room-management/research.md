# Research: FR-08 Room Management

**Date**: 2026-07-23 (refresh after clarify)  
**Spec**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

All Technical Context unknowns resolved — no remaining NEEDS CLARIFICATION.

## 1. Table Ownership & Schema Evolution

**Decision**: Own `rooms` + `room_images`. Add nullable `deleted_at` (DATETIME2) via Flyway/`SchemaColumnPatcher` on existing SQL Server schema (tables already present in codebase).

**Rationale**: Soft-delete clarification; avoid breaking booking FK.

**Alternatives considered**: Hard-delete + nullify booking.room_id — rejected (history integrity).

## 2. Unique Room Number Scope

**Decision**: Unique `(property_id, room_number)` among **active** rooms (`deleted_at IS NULL`). Prefer filtered unique index if SQL Server edition supports; else enforce in service + catch DB unique violation as 409. Soft-deleted numbers MAY be reused.

**Rationale**: FR-007; avoid permanent number lock after soft-delete.

**Alternatives considered**: Keep unique including soft-deleted — rejected (blocks reuse of "101").

## 3. Soft-Delete & Active Booking Guard

**Decision**: Soft-delete sets `deletedAt = now()`. Block when any booking status in:

`PENDING_DEPOSIT`, `CONFIRMED`, `CHECKED_IN`, `PENDING_INSPECTION`, `PENDING_DAMAGE_PAYMENT`

Align with existing `hasBlockingBookings` / overlap queries (`NOT IN CANCELLED, CHECKED_OUT, NO_SHOW`). **Replace** legacy `hasActiveBookings` that uses `COMPLETED` (non-canonical).

**Rationale**: Spec FR-004 + clarify Session 2026-07-23; SC-002.

**Alternatives considered**: Hard-delete; block on any historical booking — both rejected.

## 4. Default Status on Create

**Decision**: Always set `AVAILABLE` in service; ignore client status field if sent.

**Rationale**: FR-005 clarify.

**Alternatives considered**: Manager picks Maintenance on create — rejected (FR-05).

## 5. Manager API Surface

**Decision**: Keep `/api/v1/manager/rooms` + `/images/*`; **add** `DELETE /api/v1/manager/rooms/{id}` (soft). Prefer v1 over legacy `/api/rooms`.

| Method | Path | Notes |
|--------|------|-------|
| GET | `/api/v1/manager/rooms` | Exclude `deletedAt != null` |
| POST | `/api/v1/manager/rooms` | Status AVAILABLE |
| GET/PUT | `/api/v1/manager/rooms/{id}` | 404 if soft-deleted (manager list) |
| DELETE | `/api/v1/manager/rooms/{id}` | Soft-delete |
| POST/PUT/PATCH/DELETE | `.../images...` | Gallery; max 10 |

**Rationale**: SCR-29–32; clarify soft-delete.

## 6. api-spec Field Mapping

**Decision**: API `roomNumber` (primary); optional deprecated `name` alias if needed for old clients.

**Rationale**: Spec assumption.

## 7. Amenities Storage

**Decision**: JSON column via Hibernate `@JdbcTypeCode(SqlTypes.JSON)` → SQL Server (list of strings). Not PostgreSQL JSONB.

**Rationale**: Existing entity pattern; immutable stack.

**Alternatives considered**: Normalized amenity table — deferred.

## 8. Pricing v1

**Decision**: `pricePerNight` on Room only; PricingRule deferred.

**Rationale**: Spec assumptions; FR-011 snapshot on booking.

## 9. RoomImage Primary Constraint

**Decision**: Service clears other primaries in `@Transactional`; auto-promote first remaining on primary delete. DB partial unique optional.

**Rationale**: SC-005; existing `clearPrimary` query pattern.

## 10. Image Upload Limits

**Decision**: Before disk write: content-type / magic MIME ∈ `{image/jpeg, image/png, image/webp}`; size ≤ `UploadLimits` **10MB**; reject if `count(images) >= 10`. Store under `/uploads/rooms/{roomId}/`.

**Rationale**: FR-012 + constitution IV + LESSON-007; clarify max 10.

**Alternatives considered**: Unlimited gallery; image/* wildcard — rejected.

## 11. Employee Read Endpoint

**Decision**: `GET /api/v1/employee/rooms` (or manager GET with EMPLOYEE role read-only UI) + property assignment; writes denied 403.

**Rationale**: US-6 / SCR-65.

## 12. Floor FK Validation

**Decision**: Create/update: `floor.propertyId == room.propertyId`; Occupied rooms MAY change floor within property.

**Rationale**: Clarify Q5; FR-003.

## 13. Soft-Delete vs Cascade Images

**Decision**: Soft-delete room **does not** delete `room_images` rows; gallery hidden because room filtered out. Optional later: cascade soft flag. Hard-delete room_images only on explicit image DELETE.

**Rationale**: Soft-delete keeps FK; CASCADE hard-delete obsolete for room delete path.

**Alternatives considered**: Cascade delete images on room soft-delete — unnecessary for v1.

## 14. List Query Performance

**Decision**: Filter `deleted_at IS NULL`; indexes `(property_id, status)`, `(floor_id)`; unique active room number.

**Rationale**: SC-004.

## 15. FR-05 Status Boundary

**Decision**: No `PATCH .../status` in FR-08; SCR-29 Status → FR-05 SCR-33.

**Rationale**: Spec FR-010.

## 16. Frontend Migration

**Decision**: `roomsApi.ts` → v1 DELETE soft; list already manager-scoped; add delete confirm + gallery error toasts for MIME/10-cap.

**Rationale**: Gap analysis in plan.md.

## 17. roomNumber Change Policy

**Decision**: Always allow if unique among active rooms in property (even with booking history/active). Bookings key off `roomId`.

**Rationale**: Clarify Q2; FR-007.
