# Tasks: FR-20 Employee Management

**Input**: Design documents from `specs/020-employee-management/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/employee-api.yaml, quickstart.md

**Phụ thuộc**: FR-01 (JWT MANAGER/ADMIN, EMPLOYEE role, SUSPENDED login, invite email); FR-06 (properties, manager_property_assignments, PropertyScopeService). **Ranh giới**: FR-21 Housekeeping; FR-22 Employee Dashboard; FR-13 Maintenance assignment; FR-23 Inspection; hard delete v1 out of scope.

**Tests**: Không có phase test riêng per-story. Unit + integration trong Phase Polish per plan Phase G.

**Organization**: Tasks grouped by user story (US1–US6) for independent delivery.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no blocking deps)
- **[Story]**: US1–US6 maps to spec.md user stories

## Path Conventions

- **Backend**: `Backend/src/main/java/com/homestay/`
- **Backend tests**: `Backend/src/test/java/com/homestay/`
- **Frontend**: `Frontend/src/`
- **Migrations**: `Backend/src/main/resources/db/migration/`
- **Contract**: `specs/020-employee-management/contracts/employee-api.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Auth/property blockers, V036 migration, dev proxy

- [X] T001 Verify FR-01 JWT auth with **MANAGER** and **ADMIN** roles per `specs/001-user-auth/quickstart.md` (blocker)
- [X] T002 Verify FR-06 properties + manager assignments + `PropertyScopeService` per `specs/006-property-management/quickstart.md` (blocker)
- [X] T003 Create Flyway `Backend/src/main/resources/db/migration/V036__employee_property_assignments_fr20.sql` — table `employee_property_assignments`, partial unique index `uq_epa_one_active_per_employee` per `data-model.md`
- [X] T004 [P] Confirm Vite proxy `/api/v1` → backend in `Frontend/vite.config.ts`
- [X] T005 [P] Add dev seed SQL comments in V036 — sample EMPLOYEE user + ACTIVE assignment for local SCR-39 demo

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Entity, repository, DTOs, service skeleton, security — MUST complete before user story phases

**⚠️ CRITICAL**: No user story work until this phase complete

- [X] T006 [P] Create `EmployeePropertyAssignment.java` entity in `Backend/src/main/java/com/homestay/entities/EmployeePropertyAssignment.java` per `data-model.md`
- [X] T007 [P] Create `EmployeePropertyAssignmentRepository.java` in `Backend/src/main/java/com/homestay/repositories/EmployeePropertyAssignmentRepository.java` — `findActiveByEmployeeId`, `findActiveByPropertyId`, `existsActiveByEmployeeId`
- [X] T008 [P] Extend `UserRepository.java` in `Backend/src/main/java/com/homestay/repositories/UserRepository.java` — `findEmployeesByPropertyWithSearch`, `findUnassignedEmployees` (no ACTIVE epa)
- [X] T009 [P] Create `EmployeeSummaryResponse.java` in `Backend/src/main/java/com/homestay/dtos/employee/EmployeeSummaryResponse.java`
- [X] T010 [P] Create `CreateEmployeeRequest.java` in `Backend/src/main/java/com/homestay/dtos/employee/CreateEmployeeRequest.java`
- [X] T011 [P] Create `AssignEmployeeRequest.java` in `Backend/src/main/java/com/homestay/dtos/employee/AssignEmployeeRequest.java`
- [X] T012 [P] Create `UpdateEmployeeRequest.java` in `Backend/src/main/java/com/homestay/dtos/employee/UpdateEmployeeRequest.java`
- [X] T013 [P] Create `UpdateEmployeeStatusRequest.java` in `Backend/src/main/java/com/homestay/dtos/employee/UpdateEmployeeStatusRequest.java`
- [X] T014 [P] Create `ReassignEmployeeRequest.java` in `Backend/src/main/java/com/homestay/dtos/employee/ReassignEmployeeRequest.java` (P2)
- [X] T015 Create `EmployeeManagementService.java` skeleton in `Backend/src/main/java/com/homestay/services/EmployeeManagementService.java` — inject repos + `PropertyScopeService`
- [X] T016 Add `assertManagerPropertyAccess(managerId, propertyId)` reuse in `EmployeeManagementService.java` via `PropertyScopeService` from FR-06
- [X] T017 Register `hasRole('MANAGER')` for `/api/v1/manager/employees/**` and `hasRole('ADMIN')` for `/api/v1/admin/employees/**` in `Backend/src/main/java/com/homestay/configs/SecurityConfig.java`
- [X] T018 [P] Create `employeeApi.ts` skeleton in `Frontend/src/api/employeeApi.ts` — typed methods stub per `contracts/employee-api.yaml`

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Manager xem danh sách Employee theo Property (Priority: P1) 🎯 MVP

**Goal**: SCR-39 Staff Directory — paginated employee list scoped by property with search

**Independent Test**: Manager `GET /manager/employees?propertyId=` returns only ACTIVE assignments at property; cross-property 403; empty state when none

### Implementation

- [X] T019 [US1] Implement `listByProperty(propertyId, search, page, size, actor)` join users + epa in `EmployeeManagementService.java`
- [X] T020 [US1] Create `ManagerEmployeeController.java` with `GET /api/v1/manager/employees` in `Backend/src/main/java/com/homestay/controllers/ManagerEmployeeController.java`
- [X] T021 [US1] Create `AdminEmployeeController.java` with `GET /api/v1/admin/employees` (optional `propertyId` filter) in `Backend/src/main/java/com/homestay/controllers/AdminEmployeeController.java`
- [X] T022 [P] [US1] Implement `listManager(propertyId, params)` and `listAdmin(params)` in `Frontend/src/api/employeeApi.ts`
- [X] T023 [US1] Create `EmployeeMgmtPage.tsx` Staff Directory table (Name, Email, Phone, Status, Assigned date) in `Frontend/src/pages/manager/EmployeeMgmtPage.tsx`
- [X] T024 [US1] Wire property context from Manager session/selector on `EmployeeMgmtPage.tsx` — pass `propertyId` to API
- [X] T025 [US1] Add search input + pagination on `EmployeeMgmtPage.tsx`
- [X] T026 [US1] Add empty state + **Assign Employee** CTA on `EmployeeMgmtPage.tsx`
- [X] T027 [US1] Register `/manager/employees` with `ProtectedRoute role="MANAGER"` in `Frontend/src/App.tsx`
- [X] T028 [US1] Add **Employees** nav link to `/manager/employees` in `Frontend/src/layouts/ManagerLayout.tsx`

**Checkpoint**: US1 MVP — Manager Staff Directory testable via `quickstart.md` list curl

---

## Phase 4: User Story 2 — Admin hoặc Manager gán Employee vào Property (Priority: P1)

**Goal**: Assign existing unassigned EMPLOYEE to property via modal — one ACTIVE assignment rule

**Independent Test**: `POST /manager/employees/assign` succeeds for unassigned employee; 409 when already assigned; Manager wrong property 403

### Implementation

- [X] T029 [US2] Implement `listUnassigned(search, page, size)` in `EmployeeManagementService.java`
- [X] T030 [US2] Implement `assignEmployee(employeeId, propertyId, assignedBy)` — reject SUSPENDED, duplicate ACTIVE, inactive property in `EmployeeManagementService.java`
- [X] T031 [US2] Add `GET /api/v1/manager/employees/unassigned` in `ManagerEmployeeController.java`
- [X] T032 [US2] Add `POST /api/v1/manager/employees/assign` in `ManagerEmployeeController.java`
- [X] T033 [US2] Add `GET /api/v1/admin/employees/unassigned` and `POST /api/v1/admin/employees/assign` in `AdminEmployeeController.java`
- [X] T034 [P] [US2] Add `listUnassigned()` and `assign()` to `Frontend/src/api/employeeApi.ts`
- [X] T035 [US2] Create `AssignEmployeeModal.tsx` with unassigned employee picker in `Frontend/src/components/employee/AssignEmployeeModal.tsx`
- [X] T036 [US2] Wire **Assign Employee** button → modal → `employeeApi.assign` → reload table on `EmployeeMgmtPage.tsx`
- [X] T037 [US2] Show API error for 409 duplicate assignment on `AssignEmployeeModal.tsx`

**Checkpoint**: US2 testable — assign existing employee end-to-end

---

## Phase 5: User Story 3 — Admin hoặc Manager tạo tài khoản Employee mới (Priority: P1)

**Goal**: Create EMPLOYEE user + ACTIVE assignment + invite email in one flow

**Independent Test**: `POST /manager/employees` creates user role EMPLOYEE; appears in list; duplicate email 409; invite triggered

### Implementation

- [X] T038 [US3] Implement `createEmployee(request, assignedBy)` — transactional user create + epa ACTIVE + FR-01 invite email in `EmployeeManagementService.java`
- [X] T039 [US3] Add `POST /api/v1/manager/employees` in `ManagerEmployeeController.java`
- [X] T040 [US3] Add `POST /api/v1/admin/employees` in `AdminEmployeeController.java`
- [X] T041 [P] [US3] Add `create()` to `Frontend/src/api/employeeApi.ts`
- [X] T042 [US3] Create `CreateEmployeeModal.tsx` (fullName, email, phone, propertyId) in `Frontend/src/components/employee/CreateEmployeeModal.tsx`
- [X] T043 [US3] Wire create flow from `EmployeeMgmtPage.tsx` — open modal, validate, submit, reload list
- [X] T044 [US3] Display validation errors for required fields and duplicate email on `CreateEmployeeModal.tsx`

**Checkpoint**: US3 testable — create + assign new employee

---

## Phase 6: User Story 4 — Admin hoặc Manager cập nhật thông tin Employee (Priority: P1)

**Goal**: Update fullName and phone; email immutable v1; property scope enforced

**Independent Test**: Manager updates phone for employee at assigned property; cross-property update denied

### Implementation

- [X] T045 [US4] Implement `updateEmployee(employeeId, request, actor)` — fullName/phone only; scope check in `EmployeeManagementService.java`
- [X] T046 [US4] Add `PUT /api/v1/manager/employees/{id}` in `ManagerEmployeeController.java`
- [X] T047 [US4] Add `PUT /api/v1/admin/employees/{id}` in `AdminEmployeeController.java`
- [X] T048 [P] [US4] Add `update(id, body)` to `Frontend/src/api/employeeApi.ts`
- [X] T049 [US4] Add edit row action / edit modal for fullName and phone on `EmployeeMgmtPage.tsx`
- [X] T050 [US4] Reject role change attempts in service layer — guard `users.role = EMPLOYEE` only in `EmployeeManagementService.java`

**Checkpoint**: US4 testable — update employee contact info

---

## Phase 7: User Story 5 — Admin hoặc Manager suspend/activate Employee (Priority: P1)

**Goal**: Toggle ACTIVE ↔ SUSPENDED with confirmation; FR-01 login block; audit log

**Independent Test**: Suspend employee → login fails; activate restores; status badge updates on list

### Implementation

- [X] T051 [US5] Implement `updateEmployeeStatus(employeeId, status, actor)` — ACTIVE/SUSPENDED only + scope in `EmployeeManagementService.java`
- [X] T052 [US5] Log `EMPLOYEE_STATUS_CHANGED` via `ActivityLogService` (FR-17 pattern) in `EmployeeManagementService.java`
- [X] T053 [US5] Add `PATCH /api/v1/manager/employees/{id}/status` in `ManagerEmployeeController.java`
- [X] T054 [US5] Add `PATCH /api/v1/admin/employees/{id}/status` in `AdminEmployeeController.java`
- [X] T055 [P] [US5] Add `updateStatus(id, status)` to `Frontend/src/api/employeeApi.ts`
- [X] T056 [US5] Add Suspend/Activate action with confirmation dialog on `EmployeeMgmtPage.tsx`
- [X] T057 [US5] Update status badge styling on table rows after PATCH on `EmployeeMgmtPage.tsx`

**Checkpoint**: US5 testable — suspend blocks login per FR-01

---

## Phase 8: User Story 6 — Admin chuyển Employee sang Property khác (Priority: P2)

**Goal**: Admin-only reassign — deactivate old ACTIVE, activate new property; warning if open tasks

**Independent Test**: `PATCH /admin/employees/{id}/reassign` moves employee A→B; Manager reassign denied

### Implementation

- [X] T058 [US6] Implement `reassignEmployee(employeeId, newPropertyId, adminId)` — transaction INACTIVE old + ACTIVE new in `EmployeeManagementService.java`
- [X] T059 [US6] Add `PATCH /api/v1/admin/employees/{id}/reassign` in `AdminEmployeeController.java`
- [X] T060 [P] [US6] Add `reassign(id, propertyId)` to `Frontend/src/api/employeeApi.ts`
- [X] T061 [US6] Create `Frontend/src/pages/admin/EmployeeMgmtPage.tsx` — reuse table + Admin property picker filter
- [X] T062 [US6] Register `/admin/employees` with `ProtectedRoute role="ADMIN"` in `Frontend/src/App.tsx`
- [X] T063 [US6] Add **Employees** nav link in `Frontend/src/layouts/AdminLayout.tsx` (create layout if missing)
- [X] T064 [US6] Add **Reassign Property** admin action with warning dialog on `Frontend/src/pages/admin/EmployeeMgmtPage.tsx`
- [X] T065 [US6] Verify Manager cannot call reassign endpoint — 403 in `EmployeeManagementControllerIT.java` stub or manual quickstart

**Checkpoint**: US6 testable — Admin cross-property reassign

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Tests, quickstart validation, RBAC hardening

- [X] T066 [P] Unit test one-ACTIVE constraint and assign rejects SUSPENDED in `Backend/src/test/java/com/homestay/unit/EmployeeManagementServiceTest.java`
- [X] T067 [P] Unit test Manager property scope denial in `EmployeeManagementServiceTest.java`
- [X] T068 Integration test Manager list/assign/create RBAC + cross-property 403 in `Backend/src/test/java/com/homestay/integration/EmployeeManagementControllerIT.java`
- [X] T069 [P] Assert duplicate assign returns 409 in `EmployeeManagementControllerIT.java`
- [X] T070 [P] Assert Customer/Employee 403 on `/manager/employees` in `EmployeeManagementControllerIT.java`
- [X] T071 Run curl smoke tests in `specs/020-employee-management/quickstart.md` and fix gaps
- [X] T072 [P] Log `EMPLOYEE_ASSIGNED` and `EMPLOYEE_CREATED` audit events in `EmployeeManagementService.java`
- [X] T073 [P] Verify email field read-only on edit UI on `EmployeeMgmtPage.tsx`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: After FR-01 + FR-06 blockers
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **US1 (P1)**: After Foundational — **MVP** list + SCR-39 page
- **US2 (P1)**: After US1 (page exists for reload) — assign modal
- **US3 (P1)**: After US2 (shared modals/page) — create flow
- **US4 (P1)**: After US1 — edit on same page
- **US5 (P1)**: After US1 — status toggle on same page
- **US6 (P2)**: After US1–US5 — Admin page + reassign
- **Polish (Phase 9)**: After desired user stories complete

### User Story Dependencies

```text
Foundational → US1 (list + Manager page) — MVP
            → US2 (assign existing)
            → US3 (create new)
            → US4 (update profile)
            → US5 (suspend/activate)
            → US6 (Admin reassign P2)
```

### Parallel Opportunities

**Phase 1**: T004, T005 [P]  
**Phase 2** (after T015): T006–T014, T018 all [P]  
**US1**: T022 parallel with T019–T021  
**US2**: T034 parallel with T029–T033  
**US3**: T041 parallel with T038–T040  
**US4**: T048 parallel with T045–T047  
**US5**: T055 parallel with T051–T054  
**US6**: T060 parallel with T058–T059  
**Polish**: T066, T067, T069, T070, T072, T073 all [P]

### Parallel Example: User Story 1

```bash
T019–T021 Backend list + Manager/Admin controllers
T022 employeeApi.ts list methods (parallel when DTOs stable)
T023–T028 Manager EmployeeMgmtPage + route + nav
```

### Parallel Example: Foundational

```bash
T006–T014 Entity, repo, DTOs (all parallel)
T015–T016 EmployeeManagementService + scope
T017 SecurityConfig
T018 employeeApi.ts skeleton
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T005)
2. Complete Phase 2: Foundational (T006–T018)
3. Complete Phase 3: User Story 1 (T019–T028)
4. **STOP and VALIDATE**: Manager Staff Directory per `quickstart.md`
5. Demo SCR-39 list with search and empty state

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 → Manager Staff Directory (MVP)
3. US2 → Assign existing employee
4. US3 → Create new employee + invite
5. US4 → Update contact info
6. US5 → Suspend/activate
7. US6 → Admin reassign + admin page
8. Polish → Tests + quickstart

### Parallel Team Strategy

With multiple developers after Foundational:

- **Developer A**: US1 + US2 (list + assign Backend/frontend)
- **Developer B**: US3 + US4 (create + update)
- **Developer C**: US5 + US6 (status + admin reassign)

---

## Notes

- Frontend **greenfield** — no existing Employee pages; share components between Manager/Admin where possible
- `PropertyScopeService` from FR-06 is blocker — implement minimal validator in FR-20 if FR-06 incomplete
- FR-01 invite email on create — stub with log if mail not configured in dev
- Hard delete out of scope v1 — Suspend only
- Housekeeping/Maintenance task assignment belongs FR-21/FR-13 — not FR-20
