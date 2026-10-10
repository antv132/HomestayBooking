# Tasks: FR-07 Structure Management

**Input**: Design documents from `specs/007-structure-management/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/structure-api.yaml, quickstart.md

**Phụ thuộc**: FR-06 (Property + ManagerPropertyAssignment scope). **Ranh giới**: FR-08 Room CRUD; FR-05 8-status badges (optional enrich); FR-03 discovery.

**Tests**: Không có phase test riêng per-story. Unit + integration trong Phase Polish per plan Phase J.

**Organization**: Tasks grouped by user story (US1–US5) for independent delivery.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no blocking deps)
- **[Story]**: US1–US5 maps to spec.md user stories

## Path Conventions

- **Backend**: `Backend/src/main/java/com/homestay/`
- **Backend tests**: `Backend/src/test/java/com/homestay/`
- **Frontend**: `Frontend/src/`
- **Migrations**: `Backend/src/main/resources/db/migration/`
- **Contract**: `specs/007-structure-management/contracts/structure-api.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Flyway floors table — after FR-06 V005–V007

- [X] T001 Verify backend Spring Boot scaffold and FR-06 migrations applied per `specs/006-property-management/quickstart.md`
- [X] T002 [P] Confirm Vite proxy `/api/v1` → backend in `Frontend/vite.config.ts`
- [X] T003 Create Flyway `Backend/src/main/resources/db/migration/V008__floors.sql` — `floors` table + `UNIQUE (property_id, floor_number)` per `data-model.md`
- [X] T004 [P] Create optional seed `Backend/src/main/resources/db/migration/V008_1__structure_seed.sql` — 2 floors on FR-06 seed property for dev/quickstart (skip if no V007 seed yet)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Floor entity, DTOs, repositories, security routes — MUST complete before user story phases

**⚠️ CRITICAL**: No user story work until this phase complete

- [X] T005 [P] Create `Floor.java` entity in `Backend/src/main/java/com/homestay/entities/Floor.java`
- [X] T006 [P] Create `FloorRepository.java` in `Backend/src/main/java/com/homestay/repositories/FloorRepository.java` — include `existsByPropertyIdAndFloorNumber`, `countByPropertyIdAndFloorNumberExcludingId`
- [X] T007 [P] Create structure DTOs in `Backend/src/main/java/com/homestay/dtos/structure/` — `CreateFloorRequest`, `UpdateFloorRequest`, `FloorResponse`, `StructureTreeResponse`, `FloorTreeNode`, `RoomTreeNode` per `contracts/structure-api.yaml`
- [X] T008 Create `FloorService.java` skeleton in `Backend/src/main/java/com/homestay/services/FloorService.java` — inject FloorRepository, PropertyAccessValidator, optional RoomRepository
- [X] T009 Create `StructureTreeService.java` skeleton in `Backend/src/main/java/com/homestay/services/StructureTreeService.java`
- [X] T010 Register `/api/v1/properties/*/tree`, `/api/v1/manager/floors/**`, `/api/v1/admin/properties/*/tree` in `Backend/src/main/java/com/homestay/configs/SecurityConfig.java`
- [X] T011 [P] Migrate `Frontend/src/api/floorApi.ts` path prefix to `/api/v1/manager/floors` (keep method signatures)
- [X] T012 [P] Add `fetchStructureTree(propertyId)` in `Frontend/src/api/propertyApi.ts` calling `GET /api/v1/properties/{id}/tree`; keep `getStructure` as alias to tree during migration

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Manager xem cây cấu trúc (Priority: P1) 🎯 MVP

**Goal**: SCR-28 — Property Selector (assigned only) + tree Property → Floor → Room read-only

**Independent Test**: Manager opens `/manager/structure` → selects property → tree shows floors sorted by floorNumber; rooms as leaf nodes; unassigned property → 403

### Implementation

- [X] T013 [US1] Implement `buildTree(propertyId)` in `StructureTreeService.java` — load floors ORDER BY floorNumber; LEFT JOIN rooms (if table exists) ORDER BY roomNumber per `data-model.md`
- [X] T014 [US1] Create `StructureTreeController.java` with `GET /api/v1/properties/{propertyId}/tree` in `Backend/src/main/java/com/homestay/controllers/StructureTreeController.java`
- [X] T015 [US1] Enforce `PropertyAccessValidator.assertManagerAssigned` on manager tree GET in `StructureTreeController.java`
- [X] T016 [US1] Add legacy alias `GET /api/v1/properties/{propertyId}/structure` returning same payload in `StructureTreeController.java`
- [X] T017 [US1] Return empty `floors[]` with empty-state friendly response when no floors in `StructureTreeService.java`
- [X] T018 [P] [US1] Migrate property selector in `Frontend/src/pages/manager/StructureTreePage.tsx` to use `propertyApi.getAll` → `/api/v1/manager/properties` (FR-06 path)
- [X] T019 [US1] Wire tree load to `fetchStructureTree` / `getStructure` in `Frontend/src/pages/manager/StructureTreePage.tsx` — replace legacy `/api/properties/{id}/structure`
- [X] T020 [P] [US1] Display property address in selector dropdown on `StructureTreePage.tsx` to disambiguate same-name properties per spec Edge Cases
- [X] T021 [US1] Handle 403/404 errors with user-friendly message on `StructureTreePage.tsx`
- [X] T022 [P] [US1] Add empty-state UI "Add your first floor" when `floors.length===0` on `StructureTreePage.tsx`

**Checkpoint**: US1 MVP — Manager can view structure tree for assigned property

---

## Phase 4: User Story 2 — Manager thêm tầng (Priority: P1)

**Goal**: Add Floor via modal — floorNumber + description; reject duplicate number

**Independent Test**: POST floor → appears on tree; duplicate floorNumber → 409; floorNumber ≤0 → 400

### Implementation

- [X] T023 [US2] Implement `createFloor(CreateFloorRequest, managerId)` with default validation in `FloorService.java`
- [X] T024 [US2] Reject duplicate `(propertyId, floorNumber)` with 409 in `FloorService.java`
- [X] T025 [US2] Create `ManagerFloorController.java` with `POST /api/v1/manager/floors` in `Backend/src/main/java/com/homestay/controllers/ManagerFloorController.java`
- [X] T026 [US2] Apply `@PropertyAccess` on create using `propertyId` from body in `ManagerFloorController.java`
- [X] T027 [US2] Wire `FloorModal` add mode to migrated `floorApi.create` in `Frontend/src/pages/manager/StructureTreePage.tsx`
- [X] T028 [US2] Refresh tree after successful floor create in `StructureTreePage.tsx`
- [ ] T029 [US2] Log `FLOOR_CREATED` to ActivityLog in `FloorService.java` (**deferred** — chưa có ActivityLog wiring FR-07)

**Checkpoint**: US2 testable — Manager can add floors

---

## Phase 5: User Story 3 — Manager chỉnh sửa tầng (Priority: P1)

**Goal**: Edit floor number and description via modal

**Independent Test**: PUT update description → tree reflects; change floorNumber to duplicate → 409

### Implementation

- [X] T030 [US3] Implement `updateFloor(floorId, UpdateFloorRequest, managerId)` in `FloorService.java`
- [X] T031 [US3] Validate unique floorNumber on update excluding self in `FloorService.java`
- [X] T032 [US3] Add `PUT /api/v1/manager/floors/{id}` in `ManagerFloorController.java`
- [X] T033 [US3] Resolve floor → propertyId for access check on update in `ManagerFloorController.java`
- [X] T034 [US3] Wire `FloorModal` edit mode to `floorApi.update` in `Frontend/src/pages/manager/StructureTreePage.tsx`
- [X] T035 [US3] Refresh tree after successful floor update in `StructureTreePage.tsx`
- [ ] T036 [US3] Log `FLOOR_UPDATED` in `FloorService.java` (**deferred** — ActivityLog)

**Checkpoint**: US3 testable — Manager can edit floors

---

## Phase 6: User Story 4 — Manager xóa tầng (Priority: P1)

**Goal**: Delete floor only when no child rooms

**Independent Test**: DELETE empty floor → 200 + removed from tree; floor with rooms → 409

### Implementation

- [X] T037 [US4] Implement `deleteFloor(floorId, managerId)` — check `roomRepository.countByFloorId(floorId)==0` in `FloorService.java`
- [X] T038 [US4] Return 409 "Floor has rooms" when count > 0 in `FloorService.java`
- [X] T039 [US4] Add `DELETE /api/v1/manager/floors/{id}` in `ManagerFloorController.java`
- [X] T040 [US4] Wire delete floor action + confirm dialog on `Frontend/src/pages/manager/StructureTreePage.tsx`
- [X] T041 [US4] Display API 409 message when delete blocked in `StructureTreePage.tsx`
- [ ] T042 [US4] Log `FLOOR_DELETED` in `FloorService.java` (**deferred** — ActivityLog)

**Checkpoint**: US4 testable — delete rules enforced

---

## Phase 7: User Story 5 — Admin xem cấu trúc read-only (Priority: P2)

**Goal**: Admin views tree on any property; no floor CRUD actions

**Independent Test**: Admin GET tree any property → 200; Admin POST floor → 403; UI hides Add/Edit/Delete

### Implementation

- [X] T043 [US5] Add `GET /api/v1/admin/properties/{propertyId}/tree` delegating to `StructureTreeService` in `StructureTreeController.java` — ADMIN role only
- [X] T044 [US5] Deny Admin role on `POST/PUT/DELETE /manager/floors` in `ManagerFloorController.java` and `SecurityConfig.java`
- [X] T045 [US5] Add `readOnly` prop to `StructureTreePage.tsx` — hide Add Floor, Edit, Delete when true
- [X] T046 [US5] Create admin route `/admin/structure` reusing `StructureTreePage` with `readOnly` and admin property selector (`adminPropertyApi.getAll`) in `Frontend/src/App.tsx`
- [X] T047 [P] [US5] Register `ProtectedRoute role="ADMIN"` for `/admin/structure` in `Frontend/src/App.tsx`
- [X] T048 [US5] Add optional admin nav link in `Frontend/src/layouts/AdminLayout.tsx` to `/admin/structure`

**Checkpoint**: US5 testable — Admin read-only structure view

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Floor list endpoint for RoomListPage, tests, status badges, quickstart

- [X] T049 Add `GET /api/v1/manager/floors?propertyId=` returning `FloorResponse[]` with `roomCount` in `ManagerFloorController.java` — used by `RoomListPage.tsx`
- [X] T050 [P] Verify `Frontend/src/pages/manager/RoomListPage.tsx` floor filter works with migrated `floorApi.getByProperty`
- [X] T051 [P] Extend room status config on `StructureTreePage.tsx` to 8 statuses using `Frontend/src/utils/roomCalendar.ts` when FR-05 available (fallback keep 5)
- [X] T052 [P] Optional room node link to `/manager/rooms/{id}` on `StructureTreePage.tsx` per spec Assumptions
- [X] T053 Unit test `FloorServiceTest.java` in `Backend/src/test/java/com/homestay/unit/FloorServiceTest.java` — duplicate number + delete with rooms
- [ ] T054 Integration test `StructureTreeControllerIT.java` — covered by `FloorServiceTest` unit; full IT optional
- [ ] T055 Run curl smoke tests in `specs/007-structure-management/quickstart.md` — unit tests green; restart BE rồi smoke UI nếu cần
- [X] T056 [P] Verify `Frontend/src/App.tsx` routes `/manager/structure` and `/admin/structure` with correct role guards

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: After FR-06 V005–V007 (or parallel if properties stub exists)
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **US1 (P1)**: After Foundational — tree read MVP
- **US2 (P1)**: After US1 tree exists (visual verification); can parallel backend POST with US1 backend
- **US3 (P1)**: After US2 create path (floor records exist)
- **US4 (P1)**: After US2; benefits from FR-08 rooms for 409 delete test
- **US5 (P2)**: After US1 tree service
- **Polish**: After US1–US4 minimum

### User Story Dependencies

| Story | Depends on | Independent test |
|-------|------------|------------------|
| US1 | Foundational + FR-06 scope | GET tree + SCR-28 UI |
| US2 | Foundational, US1 tree refresh | POST floor + modal |
| US3 | US2 (floor exists) | PUT floor |
| US4 | US2 | DELETE empty floor; 409 with rooms |
| US5 | US1 StructureTreeService | Admin GET; no write UI |

### Parallel Opportunities

- Phase 1: T002, T004 parallel
- Phase 2: T005–T007, T011–T012 parallel
- US1: T018, T020, T022 parallel after T014
- US5: T047, T048 parallel
- Polish: T050, T051, T052, T056 parallel

---

## Parallel Example: Foundational Phase

```bash
T005 Floor.java | T006 FloorRepository.java | T007 DTOs package
T011 floorApi.ts migration | T012 propertyApi.ts fetchStructureTree
```

---

## Parallel Example: User Story 1

```bash
# After T014 controller:
T018 StructureTreePage property selector migration
T020 address in dropdown
T022 empty state UI
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 + Phase 2
2. Phase 3 (US1) — structure tree view SCR-28
3. **STOP and VALIDATE**: quickstart GET tree + UI selector
4. Demo empty property → add floor in US2 next

### Incremental Delivery

1. Setup + Foundational
2. US1 → tree view MVP
3. US2 → add floor
4. US3 → edit floor
5. US4 → delete floor
6. US5 → admin read-only
7. Polish → tests + RoomListPage floor filter

### Suggested MVP Scope

**T001–T022** (Setup + Foundational + US1) — Manager structure tree SCR-28.

### Full Feature Scope

**T001–T056** — Complete FR-07 including floor CRUD, admin read-only, and integration tests.

---

## Notes

- V008 must run after FR-06 `V005__properties.sql`
- `RoomRepository` optional until FR-08 — delete check returns 0 rooms if table empty
- Do not implement room CRUD in FR-07 — tree room nodes are read-only
- `FloorManagementPage` route optional; SCR-28 modal is primary UX
- Commit after each phase checkpoint
