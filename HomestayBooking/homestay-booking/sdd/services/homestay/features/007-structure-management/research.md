# Research: FR-07 Structure Management

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Docs**: `docs/Specification_v2.md` FR-07, `docs/api-spec-by-screen.md` SCR-28, frontend `StructureTreePage.tsx`, `floorApi.ts`

## 1. Floor Table Ownership

**Decision**: FR-07 creates `floors` table in Flyway **V008** (after FR-06 V005–V007, before FR-08 `rooms` V009 if separate).

**Rationale**: §5 Floor entity; FR-06 property detail stats reference floors count.

**Alternatives considered**: Embed floors in JSON on Property — rejected (normalized hierarchy).

## 2. Unique Floor Number per Property

**Decision**: DB unique constraint `UNIQUE (property_id, floor_number)` + service-level check before save.

**Rationale**: spec FR-006, SC-002.

**Alternatives considered**: Application-only — rejected (race on concurrent create).

## 3. Delete Floor Rule

**Decision**: `DELETE` allowed only when `COUNT(rooms WHERE floor_id = ?) = 0`. Return 409 with message if rooms exist.

**Rationale**: spec FR-005, US-4; FR-08 owns room deletion.

**Alternatives considered**: Cascade delete rooms — rejected (dangerous, FR-08 scope).

## 4. Structure Tree API Shape

**Decision**: `GET /api/v1/properties/{propertyId}/tree` returns:

```json
{
  "propertyId": "uuid",
  "propertyName": "string",
  "floors": [{
    "id": "uuid",
    "floorNumber": 1,
    "description": "...",
    "roomCount": 3,
    "rooms": [{
      "id": "uuid",
      "roomNumber": "101",
      "roomType": "DELUXE",
      "status": "AVAILABLE"
    }]
  }]
}
```

Sorted by `floorNumber ASC`; rooms by `roomNumber ASC`. Align api-spec SCR-28 (`name` → `floorNumber` label in UI).

**Rationale**: Matches frontend `PropertyStructure` / `FloorNode` / `RoomNode` in `propertyApi.ts`.

**Alternatives considered**: Separate floor list + room list calls — rejected (extra round trips, SC-005).

## 5. Legacy Path Compatibility

**Decision**: Implement primary `GET /api/v1/properties/{id}/tree`. Optional alias `GET .../structure` returning same payload during migration (frontend currently uses `/structure`).

**Rationale**: Minimize frontend breakage; deprecate alias in FR-08 cleanup.

**Alternatives considered**: Break frontend immediately — acceptable but document in quickstart.

## 6. Floor CRUD API Surface

**Decision**:

| Method | Path | Role |
|--------|------|------|
| GET | `/api/v1/manager/floors?propertyId=` | MANAGER (scoped) |
| POST | `/api/v1/manager/floors` | MANAGER |
| PUT | `/api/v1/manager/floors/{id}` | MANAGER |
| DELETE | `/api/v1/manager/floors/{id}` | MANAGER |

Body: `{ propertyId, floorNumber, description? }` on create.

**Rationale**: Separates floor writes from tree read; matches existing `floorApi.ts` shape.

**Alternatives considered**: Nested `/properties/{id}/floors` — also valid; manager path keeps FR-06 controller split consistent.

## 7. Property Access Control

**Decision**: All Manager floor/tree operations call `PropertyAccessValidator.assertManagerAssigned(userId, propertyId)`. Admin tree GET skips assignment check; Admin floor POST/PUT/DELETE **denied** (403).

**Rationale**: spec FR-007, FR-009; permission matrix Admin R only.

**Alternatives considered**: Admin can edit floors — rejected (spec US-5 read-only).

## 8. Property Selector Data Source

**Decision**: Manager selector uses `GET /api/v1/manager/properties` (FR-06). Admin selector uses `GET /api/v1/admin/properties` (all properties, paginated or lightweight list).

**Rationale**: spec FR-002 vs US-5; reuse FR-06 API.

**Alternatives considered**: Dedicated selector endpoint — YAGNI.

## 9. Room Nodes Without FR-08

**Decision**: Tree service left-joins `rooms` when table exists; returns `rooms: []` per floor if no rows. No stub fake rooms.

**Rationale**: FR-07 read-only display; honest empty state until FR-08.

**Alternatives considered**: Block FR-07 until FR-08 — rejected (floors usable without rooms).

## 10. Frontend Strategy

**Decision**: Keep `StructureTreePage.tsx` as SCR-28 implementation; migrate API calls only. Add `readOnly` prop when `user.role === 'ADMIN'` hiding Add/Edit/Delete floor actions (US-5).

**Rationale**: ~900 lines UI already built; spec Assumptions modal CRUD on same page.

**Alternatives considered**: New admin-only page — duplicate; shared component preferred.

## 11. Floor List by Property (Auxiliary)

**Decision**: `GET /manager/floors?propertyId=` returns `FloorSummary[]` with `roomCount` for RoomListPage floor filter (FR-08 consumes same endpoint).

**Rationale**: `RoomListPage.tsx` already uses `floorApi.getByProperty`.

**Alternatives considered**: Tree-only no list endpoint — breaks room list filter.

## 12. ActivityLog Events

**Decision**: Log `FLOOR_CREATED`, `FLOOR_UPDATED`, `FLOOR_DELETED` with propertyId, floorId, actor.

**Rationale**: plan Constitution Check audit requirement.

**Alternatives considered**: Skip audit for structure — rejected.

## 13. Performance — Tree Query

**Decision**: One repository method `findFloorsWithRoomsByPropertyId` using `@Query` JOIN FETCH or native aggregation; cap implicit at property size (<500 rooms) per SC-005.

**Rationale**: SC-005 <3s for 200 rooms.

**Alternatives considered**: Lazy load rooms per floor on expand — optional frontend optimization later.

## 14. Optional FloorManagementPage

**Decision**: Route `/manager/floors` (if `FloorManagementPage` exists) reuses `floorApi.getByProperty` — **out of scope** for FR-07 v1 beyond API support; SCR-28 is primary UX.

**Rationale**: spec Assumptions optional dedicated page.

**Alternatives considered**: Remove `/manager/floors` route — defer to tasks if page redundant.
