# Research: FR-06 Property Management

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Docs**: `docs/Specification_v2.md` FR-06, §5 Property/ManagerPropertyAssignment, `docs/api-spec-by-screen.md` SCR-46–50, frontend `propertyApi.ts`, manager property pages

## 1. Table Ownership & Migration Order

**Decision**: FR-06 creates `properties` (V005) and `manager_property_assignments` (V006) **before** FR-03 discovery seed (V010) and FR-04 bookings (V020+).

**Rationale**: FR-03 data-model assumes Property table exists; properties are foundational hierarchy root.

**Alternatives considered**: Define properties in FR-03 seed only — rejected (no CRUD/admin lifecycle).

## 2. Admin vs Manager API Split

**Decision**:

| Role | Base path | Operations |
|------|-----------|------------|
| ADMIN | `/api/v1/admin/properties` | CRUD, status, assign manager, list all |
| MANAGER | `/api/v1/manager/properties` | GET list + GET detail (assigned only) |

**Rationale**: spec FR-010 Admin-only write; clear RBAC in SecurityConfig; matches api-spec SCR-46–49 vs manager read in Specification_v2 §2.

**Alternatives considered**: Single `/api/v1/properties` with role-based filtering — rejected (confusing, harder to audit).

## 3. One ACTIVE Assignment per Property

**Decision**: SQL Server filtered unique index:

```sql
CREATE UNIQUE INDEX uq_mpa_one_active_per_property
  ON manager_property_assignments (property_id)
  WHERE status = 'ACTIVE';
```

Service layer deactivates existing ACTIVE row before inserting new ACTIVE in one `@Transactional` method.

**Rationale**: spec FR-006, §5 Business Constraints; DB enforces even under concurrency.

**Alternatives considered**: Application-only check — rejected (race conditions).

## 4. Property ACTIVE Requires Manager

**Decision**: `PropertyService.updateStatus(ACTIVE)` validates `existsActiveAssignment(propertyId)`; reject 409 if none. New property defaults **INACTIVE** on POST. Optional combined flow: POST create + PATCH assign + PUT activate in UI wizard (3 calls, not one mega-endpoint).

**Rationale**: spec Assumptions + US-4; avoids ACTIVE property without manager.

**Alternatives considered**: Auto-assign default manager — rejected (no default in spec).

## 5. Assignment History

**Decision**: Never hard-delete assignment rows; set `status = INACTIVE`, retain `assigned_by`, `assigned_at`. Optional `GET /admin/properties/{id}/assignments` for audit (paginated, newest first).

**Rationale**: spec FR-007; §5 "INACTIVE assignments được giữ lại".

**Alternatives considered**: Soft-delete property only — insufficient for assignment audit.

## 6. Manager Validation on Assign

**Decision**: `AssignManagerRequest.managerId` must reference `users` where `role = MANAGER` and `status = ACTIVE`. Reject ADMIN users, SUSPENDED managers (409).

**Rationale**: spec FR-012; edge case Admin cannot assign self if role ADMIN.

**Alternatives considered**: Allow SUSPENDED with warning — rejected (FR-012 says ACTIVE).

## 7. api-spec Field Mapping

**Decision**: Map api-spec `location` → entity `address`. Response includes both `address` and deprecated alias `location` optional for compat, prefer `address` in new frontend.

**Rationale**: spec Assumption "Location trong api-spec = address trong entity §5".

**Alternatives considered**: Rename DB column to location — rejected (§5 uses Address).

## 8. Admin List Enrichment

**Decision**: `PropertySummaryResponse` includes `currentManager: { id, fullName } | null` via LEFT JOIN on active assignment + users.

**Rationale**: SCR-46 columns Name, Location, Manager, Status; US-5.

**Alternatives considered**: N+1 queries per row — rejected (single JPQL with join).

## 9. Manager Detail Stats

**Decision**: `PropertyDetailResponse.stats` aggregates `{ totalFloors, totalRooms, availableRooms, ... }` from `floors`/`rooms` when FR-07/08 tables exist; return zeros until then. Matches existing frontend `PropertyDetail` type in `propertyApi.ts`.

**Rationale**: US-3; frontend already expects stats shape.

**Alternatives considered**: Omit stats until FR-08 — rejected (breaks existing PropertyDetailPage).

## 10. Manager Frontend Refactor

**Decision**: Remove Manager routes `/manager/properties/add`, delete action, `propertyApi.create/update/delete` from manager flows. Move create/edit forms to Admin pages. Manager keeps list + detail read-only.

**Rationale**: spec FR-010; current frontend incorrectly implements Manager CRUD (SCR labels wrong: SCR-33/35 vs SCR-46–48).

**Alternatives considered**: Keep Manager create for convenience — rejected (violates spec).

## 11. Property INACTIVE & Discovery

**Decision**: `PropertyService` emits no event; FR-03 `RoomSearchService` filters `property.status = ACTIVE`. Document integration test in quickstart.

**Rationale**: spec FR-011; FR-03 already filters ACTIVE properties in research #7.

**Alternatives considered**: Cascade INACTIVE to all rooms — deferred to FR-08 (rooms inherit visibility via property join).

## 12. SCR-47 Extended Fields (v1 Scope)

**Decision**: v1 implements core §5 fields only: `name`, `address`, `description`, `status`. SCR-47 wireframe extras (map, facilities, contact, check-in policy) deferred to v2 columns or JSONB `metadata` without UI in v1.

**Rationale**: spec Assumptions "optional v1".

**Alternatives considered**: Full wireframe in v1 — scope creep.

## 13. Unassign Manager

**Decision**: No standalone "unassign" without replacement if property ACTIVE — reject 409. If property INACTIVE, allow deactivating assignment (manager optional while dormant).

**Rationale**: US-4 scenario 2; ACTIVE must have manager.

**Alternatives considered**: Auto-INACTIVE property on unassign — acceptable alternative but explicit Admin deactivate preferred.

## 14. Admin Layout & Routing

**Decision**: Add `AdminLayout`, routes under `/admin/properties/*` with `ProtectedRoute role="ADMIN"`. No admin section exists in `App.tsx` today.

**Rationale**: entity-ui-mapping Admin screens SCR-46–49; grep shows zero admin routes.

**Alternatives considered**: Reuse ManagerLayout for Admin — rejected (wrong nav IA).

## 15. Manager Directory for SCR-49

**Decision**: Reuse `GET /api/v1/admin/users?role=MANAGER&status=ACTIVE&page&size` (SCR-50) for assignment dropdown; not owned by FR-06 but consumed by AssignManagerPage.

**Rationale**: api-spec SCR-50; FR-01/17 user admin module.

**Alternatives considered**: Embed managers in property API — rejected (separation of concerns).
