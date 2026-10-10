# Tasks: FR-06 Property Management

**Input**: Design documents from `specs/006-property-management/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/property-api.yaml, quickstart.md

**Phụ thuộc**: FR-01 (Admin/Manager JWT, RBAC, `GET /admin/users?role=MANAGER` SCR-50). **Enables**: FR-03 (ACTIVE filter), FR-07/08 (Property hierarchy), FR-04/05 (`PropertyAccessValidator`). **Ranh giới**: FR-07 Structure; FR-08 Room CRUD; EmployeePropertyAssignment.

**Tests**: Không có phase test riêng per-story. Unit + integration trong Phase Polish per plan Phase L.

**Organization**: Tasks grouped by user story (US1–US5) for independent delivery.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no blocking deps)
- **[Story]**: US1–US5 maps to spec.md user stories

## Path Conventions

- **Backend**: `Backend/src/main/java/com/homestay/`
- **Backend tests**: `Backend/src/test/java/com/homestay/`
- **Frontend**: `Frontend/src/`
- **Migrations**: `Backend/src/main/resources/db/migration/`
- **Contract**: `specs/006-property-management/contracts/property-api.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Migrations and dev prerequisites — MUST run before V010 FR-03 seed

- [X] T001 Verify backend Spring Boot scaffold (or FR-01 setup) per `specs/001-user-auth/plan.md`
- [X] T002 [P] Confirm Vite proxy `/api/v1` → backend in `Frontend/vite.config.ts`
- [X] T003 Create Flyway `Backend/src/main/resources/db/migration/V005__properties.sql` per `specs/006-property-management/data-model.md`
- [X] T004 Create Flyway `Backend/src/main/resources/db/migration/V006__manager_property_assignments.sql` — partial unique index `uq_mpa_one_active_per_property` per `data-model.md`
- [X] T005 [P] Create optional dev seed `Backend/src/main/resources/db/migration/V007__property_management_seed.sql` — 2 INACTIVE properties + 1 ACTIVE with assignment for quickstart

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Entities, enums, DTOs, security routes — MUST complete before user story phases

**⚠️ CRITICAL**: No user story work until this phase complete

- [X] T006 [P] Create `PropertyStatus.java` enum in `Backend/src/main/java/com/homestay/enums/PropertyStatus.java`
- [X] T007 [P] Create `AssignmentStatus.java` enum in `Backend/src/main/java/com/homestay/enums/AssignmentStatus.java`
- [X] T008 [P] Create `Property.java` entity in `Backend/src/main/java/com/homestay/entities/Property.java`
- [X] T009 [P] Create `ManagerPropertyAssignment.java` entity in `Backend/src/main/java/com/homestay/entities/ManagerPropertyAssignment.java`
- [X] T010 [P] Create `PropertyRepository.java` in `Backend/src/main/java/com/homestay/repositories/PropertyRepository.java`
- [X] T011 [P] Create `ManagerPropertyAssignmentRepository.java` in `Backend/src/main/java/com/homestay/repositories/ManagerPropertyAssignmentRepository.java`
- [X] T012 [P] Create property DTOs in `Backend/src/main/java/com/homestay/dtos/property/` — `CreatePropertyRequest`, `UpdatePropertyRequest`, `PropertySummaryResponse`, `PropertyDetailResponse`, `AssignManagerRequest`, `ManagerAssignmentResponse`, `PropertyPageResponse` per `contracts/property-api.yaml`
- [X] T013 Accept `location` alias → `address` in `CreatePropertyRequest.java` and `UpdatePropertyRequest.java` per research.md #7
- [X] T014 Register `/api/v1/admin/properties/**` (ADMIN) and `/api/v1/manager/properties/**` (MANAGER) in `Backend/src/main/java/com/homestay/configs/SecurityConfig.java`
- [X] T015 Create or extend `PropertyAccessValidator.java` in `Backend/src/main/java/com/homestay/security/PropertyAccessValidator.java` — `assertManagerAssigned(userId, propertyId)`
- [X] T016 [P] Create `adminPropertyApi.ts` in `Frontend/src/api/adminPropertyApi.ts` — typed client skeleton for admin endpoints
- [X] T017 [P] Create `AdminLayout.tsx` in `Frontend/src/layouts/AdminLayout.tsx` — nav shell for SCR-46–49

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Admin tạo và quản lý Property (Priority: P1) 🎯 MVP

**Goal**: Admin create/edit property; default INACTIVE; status ACTIVE/INACTIVE on update (SCR-47, SCR-48)

**Independent Test**: Admin POST property → INACTIVE; PUT update name/address; PUT status INACTIVE; curl or Create/Edit pages; Manager POST → 403

### Implementation

- [X] T018 [US1] Implement `createProperty(CreatePropertyRequest, adminId)` default status INACTIVE in `Backend/src/main/java/com/homestay/services/PropertyService.java`
- [X] T019 [US1] Implement `updateProperty(id, UpdatePropertyRequest)` field updates in `PropertyService.java`
- [X] T020 [US1] Implement `updatePropertyStatus(id, status)` — allow INACTIVE always; defer ACTIVE gate to US4 in `PropertyService.java`
- [X] T021 [US1] Create `AdminPropertyController.java` with `POST /api/v1/admin/properties` in `Backend/src/main/java/com/homestay/controllers/AdminPropertyController.java`
- [X] T022 [US1] Add `PUT /api/v1/admin/properties/{id}` in `AdminPropertyController.java`
- [X] T023 [US1] Add `GET /api/v1/admin/properties/{id}` returning `PropertyDetailResponse` in `AdminPropertyController.java`
- [X] T024 [P] [US1] Implement `create`, `update`, `getById` in `Frontend/src/api/adminPropertyApi.ts`
- [X] T025 [US1] Create `CreatePropertyPage.tsx` in `Frontend/src/pages/admin/CreatePropertyPage.tsx` — SCR-47 form (name, address, description)
- [X] T026 [US1] Create `EditPropertyPage.tsx` in `Frontend/src/pages/admin/EditPropertyPage.tsx` — SCR-48 form + status toggle
- [X] T027 [P] [US1] Refactor `PropertyForm` in `Frontend/src/pages/manager/_propertyShared.tsx` for reuse by admin pages (export shared fields component)
- [X] T028 [US1] Register routes `/admin/properties/new` and `/admin/properties/:id/edit` with `ProtectedRoute role="ADMIN"` in `Frontend/src/App.tsx`
- [X] T029 [US1] Log `PROPERTY_CREATED`, `PROPERTY_UPDATED`, `PROPERTY_STATUS_CHANGED` via ActivityLog in `PropertyService.java`

**Checkpoint**: US1 MVP — Admin can create and edit properties via API + admin pages

---

## Phase 4: User Story 2 — Admin gán Manager cho Property (Priority: P1)

**Goal**: Assign/reassign Manager; one ACTIVE assignment per property; history preserved (SCR-49, SCR-50)

**Independent Test**: PATCH assign → Manager sees property; reassign → old INACTIVE + new ACTIVE; invalid managerId → 409; GET assignments history

### Implementation

- [X] T030 [US2] Create `ManagerAssignmentService.java` in `Backend/src/main/java/com/homestay/services/ManagerAssignmentService.java` — `assignManager(propertyId, managerId, assignedBy)` transactional swap
- [X] T031 [US2] Validate target user `role=MANAGER` and `status=ACTIVE` in `ManagerAssignmentService.java` — reject 409 per FR-012
- [X] T032 [US2] Deactivate existing ACTIVE assignment before insert new ACTIVE in `ManagerAssignmentService.java`
- [X] T033 [US2] Add `PATCH /api/v1/admin/properties/{id}/manager` in `AdminPropertyController.java`
- [X] T034 [US2] Add `GET /api/v1/admin/properties/{id}/assignments` paginated history in `AdminPropertyController.java`
- [X] T035 [P] [US2] Add `assignManager`, `getAssignmentHistory` in `Frontend/src/api/adminPropertyApi.ts`
- [X] T036 [US2] Create `AssignManagerPage.tsx` in `Frontend/src/pages/admin/AssignManagerPage.tsx` — SCR-49 manager picker + save
- [X] T037 [US2] Fetch manager directory `GET /api/v1/admin/users?role=MANAGER&status=ACTIVE` in `AssignManagerPage.tsx` (SCR-50)
- [X] T038 [US2] Register route `/admin/properties/:id/manager` with ADMIN guard in `Frontend/src/App.tsx`
- [X] T039 [US2] Log `MANAGER_ASSIGNED`, `MANAGER_ASSIGNMENT_ENDED` in `ManagerAssignmentService.java`
- [X] T040 [P] [US2] Add link "Assign Manager" from `Frontend/src/pages/admin/EditPropertyPage.tsx` to assign route

**Checkpoint**: US2 testable — manager assignment with history

---

## Phase 5: User Story 3 — Manager xem Property được gán (Priority: P1)

**Goal**: Manager read-only list/detail scoped to ACTIVE assignments only

**Independent Test**: Manager GET list → only assigned; GET unassigned id → 403; no create/edit/delete in UI

### Implementation

- [X] T041 [US3] Create `PropertyStatsService.java` in `Backend/src/main/java/com/homestay/services/PropertyStatsService.java` — stub zeros + empty `floors[]` until FR-07/08
- [X] T042 [US3] Create `ManagerPropertyController.java` with `GET /api/v1/manager/properties` scoped by `managerId` + ACTIVE assignment in `Backend/src/main/java/com/homestay/controllers/ManagerPropertyController.java`
- [X] T043 [US3] Add `GET /api/v1/manager/properties/{id}/detail` with `PropertyAccessValidator` in `ManagerPropertyController.java`
- [X] T044 [US3] Migrate `propertyApi.getAll` to `GET /api/v1/manager/properties` in `Frontend/src/api/propertyApi.ts`
- [X] T045 [US3] Migrate `propertyApi.getDetail` to `GET /api/v1/manager/properties/{id}/detail` in `Frontend/src/api/propertyApi.ts`
- [X] T046 [US3] Refactor `Frontend/src/pages/manager/PropertyListPage.tsx` — remove delete, remove "Add Property" button, read-only list
- [X] T047 [US3] Refactor `Frontend/src/pages/manager/PropertyDetailPage.tsx` — remove edit link; read-only detail + stats
- [X] T048 [US3] Remove routes `/manager/properties/add` and `/manager/properties/:id/edit` from `Frontend/src/App.tsx`
- [X] T049 [US3] Remove `propertyApi.create`, `propertyApi.update`, `propertyApi.delete` usages from manager pages; keep methods deprecated or delete from `propertyApi.ts`
- [X] T050 [P] [US3] Remove "Thêm Property" quick action from `Frontend/src/pages/manager/ManagerDashboardPage.tsx`
- [X] T051 [P] [US3] Update empty-state link in `Frontend/src/pages/manager/AddRoomPage.tsx` — remove `/manager/properties/add` reference

**Checkpoint**: US3 testable — manager scoped read-only property views

---

## Phase 6: User Story 4 — Ràng buộc Property phải có Manager ACTIVE (Priority: P2)

**Goal**: Cannot ACTIVE without assignment; cannot unassign while ACTIVE without replacement

**Independent Test**: PUT status ACTIVE without manager → 409; assign then ACTIVE → 200; audit query 100% ACTIVE have manager

### Implementation

- [X] T052 [US4] Enforce `existsActiveAssignment(propertyId)` before `status=ACTIVE` in `PropertyService.updatePropertyStatus()` — throw 409 in `PropertyService.java`
- [X] T053 [US4] Reject deactivating last ACTIVE assignment when property status ACTIVE in `ManagerAssignmentService.java`
- [X] T054 [US4] Allow INACTIVE property without manager assignment (no-op validation) in `PropertyService.java`
- [X] T055 [US4] Surface 409 "Assign a manager before activating" on `Frontend/src/pages/admin/EditPropertyPage.tsx` status toggle
- [X] T056 [P] [US4] Add post-assign CTA "Activate property" on `Frontend/src/pages/admin/AssignManagerPage.tsx`
- [X] T057 [US4] Create unit test `PropertyActivationServiceTest.java` in `Backend/src/test/java/com/homestay/unit/PropertyActivationServiceTest.java` — ACTIVE without manager fails

**Checkpoint**: US4 testable — business constraint enforced

---

## Phase 7: User Story 5 — Admin tra cứu danh sách Property (Priority: P2)

**Goal**: SCR-46 paginated admin table with search, status filter, manager column, row actions

**Independent Test**: GET list with search "Đà Nẵng"; filter INACTIVE; columns Name, Address, Manager, Status; links to Edit and Assign

### Implementation

- [X] T058 [US5] Add `findForAdmin(search, status, pageable)` with ILIKE on name/address in `PropertyRepository.java`
- [X] T059 [US5] Implement `listForAdmin` with `currentManager` join in `PropertyService.java`
- [X] T060 [US5] Add `GET /api/v1/admin/properties` with `page`, `size`, `search`, `status` in `AdminPropertyController.java`
- [X] T061 [P] [US5] Implement `getAll(params)` in `Frontend/src/api/adminPropertyApi.ts`
- [X] T062 [US5] Create `PropertyManagementPage.tsx` in `Frontend/src/pages/admin/PropertyManagementPage.tsx` — SCR-46 DataTable
- [X] T063 [US5] Add search debounce + status filter on `PropertyManagementPage.tsx`
- [X] T064 [US5] Add row actions Edit + Assign Manager + Create button on `PropertyManagementPage.tsx`
- [X] T065 [US5] Register route `/admin/properties` index with ADMIN guard in `Frontend/src/App.tsx`

**Checkpoint**: US5 testable — full admin property management hub SCR-46

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: FR-03 integration, tests, quickstart validation, cleanup

- [X] T066 [P] Ensure FR-03 discovery filters `properties.status = ACTIVE` in `Backend/src/main/java/com/homestay/services/RoomSearchService.java` (or equivalent FR-03 service)
- [X] T067 [P] Unit test `ManagerAssignmentServiceTest.java` in `Backend/src/test/java/com/homestay/unit/ManagerAssignmentServiceTest.java` — swap + one ACTIVE per property
- [X] T068 Integration test `AdminPropertyControllerIT.java` in `Backend/src/test/java/com/homestay/integration/AdminPropertyControllerIT.java` — create → assign → activate flow
- [X] T069 Integration test `ManagerPropertyScopeIT.java` in `Backend/src/test/java/com/homestay/integration/ManagerPropertyScopeIT.java` — 403 unassigned property
- [X] T070 Run curl smoke tests in `specs/006-property-management/quickstart.md` and fix gaps
- [X] T071 [P] Verify all admin/manager property routes and `ProtectedRoute` roles in `Frontend/src/App.tsx`
- [X] T072 [P] Delete or archive unused manager CRUD pages `Frontend/src/pages/manager/AddPropertyPage.tsx` and `Frontend/src/pages/manager/EditPropertyPage.tsx` if fully replaced by admin pages

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Start immediately — **before FR-03 seed V010**
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **US1 (P1)**: After Foundational — Admin create/edit MVP
- **US2 (P1)**: After US1 (property must exist to assign)
- **US3 (P1)**: After US2 (assignments needed for scoped manager read)
- **US4 (P2)**: After US1 + US2 (activation gate spans both services)
- **US5 (P2)**: After US1 backend exists; list UI integrates US2 assign links
- **Polish**: After desired stories

### User Story Dependencies

| Story | Depends on | Independent test |
|-------|------------|------------------|
| US1 | Foundational | POST/PUT admin property + Create/Edit pages |
| US2 | US1 | PATCH assign manager + history |
| US3 | US2 | Manager scoped GET list/detail; no CRUD UI |
| US4 | US1, US2 | ACTIVE without manager → 409 |
| US5 | US1, US2 | Admin list search/filter SCR-46 |

### Parallel Opportunities

- Phase 1: T002, T005 parallel
- Phase 2: T006–T012, T016–T017 parallel
- US1: T024, T027 parallel after T021
- US2: T035, T040 parallel
- US3: T050, T051 parallel
- US4: T056 parallel with T055
- US5: T061 parallel after T060
- Polish: T066, T067, T071, T072 parallel

---

## Parallel Example: Foundational Phase

```bash
T006 PropertyStatus.java | T007 AssignmentStatus.java | T008 Property.java | T009 ManagerPropertyAssignment.java
T010 PropertyRepository.java | T011 ManagerPropertyAssignmentRepository.java | T012 DTOs package
T016 adminPropertyApi.ts | T017 AdminLayout.tsx
```

---

## Parallel Example: User Story 2

```bash
# After T033 PATCH endpoint:
T035 adminPropertyApi assignManager
T040 EditPropertyPage link to Assign Manager
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 + Phase 2
2. Phase 3 (US1) — Admin create/edit property SCR-47/48
3. **STOP and VALIDATE**: quickstart POST property + admin Create page
4. Demo property saved as INACTIVE

### Incremental Delivery

1. Setup + Foundational
2. US1 → Admin CRUD core
3. US2 → Manager assignment SCR-49
4. US3 → Manager read-only refactor
5. US4 → Activation business rules
6. US5 → Admin list hub SCR-46
7. Polish → FR-03 filter + integration tests

### Suggested MVP Scope

**T001–T029** (Setup + Foundational + US1) — Admin can create and edit properties.

### Full Feature Scope

**T001–T072** — Complete FR-06 including SCR-46 admin hub, manager scope, and FR-03 integration.

---

## Notes

- Migration order: V005–V006 **must** precede FR-03 `V010__discovery_seed.sql`
- Manager must **not** retain create/edit/delete — spec FR-010 non-negotiable
- `PropertyAccessValidator` from FR-06 is reused by FR-04/05/07 manager endpoints
- Do not implement hard delete — INACTIVE only per spec Edge Cases
- Commit after each phase checkpoint


## Implementation notes (2026-07-23)

- Package layout: `entity/controller/service/repository/dto/security` (not entities/controllers).
- T003�T005 Flyway skipped � JPA `ddl-auto`; tables already exist.
- Manager write removed from `PropertyController`; scoped reads at `/api/v1/manager/properties`.
- Admin dual path `/api/v1/admin/properties` + legacy `/api/admin/properties`.
- Default create INACTIVE; ACTIVE requires assignment (409); assign requires MANAGER+ACTIVE user.
- Public `GET /api/properties` ACTIVE-only.
- FE: `propertyApi` ? manager v1; create defaults INACTIVE; SCR-50 ACTIVE managers; post-assign Activate CTA.
- Unit: `AdminPropertyServiceTest`. Full ITs deferred (T068�T069).

