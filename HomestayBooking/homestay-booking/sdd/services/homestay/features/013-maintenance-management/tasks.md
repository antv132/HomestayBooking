# Tasks: FR-13 Maintenance Management

**Input**: Design documents from `specs/013-maintenance-management/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/maintenance-api.yaml, quickstart.md

**Phụ thuộc**: FR-04 (active booking CONFIRMED/CHECKED_IN); FR-06 (`PropertyScopeService`, `GET /manager/employees`); FR-08 (Room → propertyId); FR-01 (JWT); FR-15 (notification delivery — FR-13 emits events only). **Ranh giới**: FR-15 notification engine; FR-08 Room calendar Maintenance lock (SCR-33) separate from ticket workflow.

**Tests**: Không có phase test riêng per-story. Unit + integration trong Phase Polish per plan Phase L.

**Organization**: Tasks grouped by user story (US1–US6) for independent delivery.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no blocking deps)
- **[Story]**: US1–US6 maps to spec.md user stories

## Path Conventions

- **Backend**: `Backend/src/main/java/com/homestay/`
- **Backend tests**: `Backend/src/test/java/com/homestay/`
- **Frontend**: `Frontend/src/`
- **Migrations**: `Backend/src/main/resources/db/migration/`
- **Contract**: `specs/013-maintenance-management/contracts/maintenance-api.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Maintenance schema, upload dir, env — after FR-04 bookings exist

- [ ] T001 Verify FR-04 `bookings` table + CONFIRMED/CHECKED_IN statuses available per `specs/004-booking-inventory/quickstart.md` (blocker)
- [ ] T002 Create Flyway `Backend/src/main/resources/db/migration/V029__maintenance_tickets_fr13.sql` — `maintenance_tickets`, `attachments` (if not exists), indexes per `data-model.md`
- [ ] T003 [P] Add `app.upload.maintenance-dir` (env `APP_UPLOADS_DIR`) in `Backend/src/main/resources/application.yml` per `quickstart.md`
- [ ] T004 [P] Confirm Vite proxy `/api/v1` → backend in `Frontend/vite.config.ts`
- [ ] T005 [P] Add optional seed row (1 OPEN ticket + CONFIRMED booking) in `V029__maintenance_tickets_fr13.sql` for local demo

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Entities, DTOs, state machine skeleton, security routes — MUST complete before user story phases

**⚠️ CRITICAL**: No user story work until this phase complete

- [ ] T006 [P] Create `MaintenanceTicketStatus.java` in `Backend/src/main/java/com/homestay/enums/MaintenanceTicketStatus.java` — OPEN, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED
- [ ] T007 [P] Create `AttachmentEntityType.java` in `Backend/src/main/java/com/homestay/enums/AttachmentEntityType.java` — MAINTENANCE, DAMAGE_ITEM
- [ ] T008 [P] Create `MaintenanceTicket.java` entity in `Backend/src/main/java/com/homestay/entities/MaintenanceTicket.java` per `data-model.md`
- [ ] T009 [P] Create `Attachment.java` entity in `Backend/src/main/java/com/homestay/entities/Attachment.java` per `data-model.md`
- [ ] T010 [P] Create `MaintenanceTicketRepository.java` in `Backend/src/main/java/com/homestay/repositories/MaintenanceTicketRepository.java` — customer/property/assignee queries + `deleted_at IS NULL`
- [ ] T011 [P] Create `AttachmentRepository.java` in `Backend/src/main/java/com/homestay/repositories/AttachmentRepository.java` — `findByEntityTypeAndEntityId`
- [ ] T012 [P] Create maintenance DTOs in `Backend/src/main/java/com/homestay/dtos/maintenance/` — `MaintenanceTicketSummaryResponse`, `MaintenanceTicketDetailResponse`, `MaintenanceTicketPageResponse`, `AssignEmployeeRequest`, `UpdateMaintenanceStatusRequest`, `CloseMaintenanceTicketRequest` per `contracts/maintenance-api.yaml`
- [ ] T013 Create `FileUploadConfig.java` in `Backend/src/main/java/com/homestay/configs/FileUploadConfig.java` — max 5MB, JPEG/PNG/WebP
- [ ] T014 Create `MaintenanceBookingValidator.java` skeleton in `Backend/src/main/java/com/homestay/services/MaintenanceBookingValidator.java`
- [ ] T015 Create `MaintenanceTicketStatusService.java` skeleton in `Backend/src/main/java/com/homestay/services/MaintenanceTicketStatusService.java`
- [ ] T016 Create `MaintenanceTicketService.java` skeleton in `Backend/src/main/java/com/homestay/services/MaintenanceTicketService.java`
- [ ] T017 Create `MaintenanceAttachmentService.java` skeleton in `Backend/src/main/java/com/homestay/services/MaintenanceAttachmentService.java`
- [ ] T018 Create `MaintenanceTicketStatusChangedEvent.java` in `Backend/src/main/java/com/homestay/events/MaintenanceTicketStatusChangedEvent.java`
- [ ] T019 Create `MaintenanceNotificationPublisher.java` skeleton in `Backend/src/main/java/com/homestay/services/MaintenanceNotificationPublisher.java`
- [ ] T020 Register maintenance role routes (`CUSTOMER`, `MANAGER`, `EMPLOYEE`, `ADMIN`) in `Backend/src/main/java/com/homestay/configs/SecurityConfig.java`

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Customer gửi yêu cầu bảo trì gắn booking (Priority: P1) 🎯 MVP

**Goal**: Customer creates ticket linked to active booking with title, description, optional images; status OPEN; SCR-22/23

**Independent Test**: Customer with Confirmed booking → POST multipart → ticket OPEN in `GET /maintenance-tickets/me`; reject inactive/wrong-owner booking

### Implementation

- [ ] T021 [US1] Implement `assertActiveForCustomer(bookingId, customerId)` — CONFIRMED/CHECKED_IN + ownership in `MaintenanceBookingValidator.java`
- [x] T022 [US1] Implement `createTicket(customerId, bookingId, title, description, files)` — denorm roomId/propertyId, status OPEN in `MaintenanceTicketService.java`
- [ ] T023 [US1] Implement `storeAttachments(ticketId, files)` max 5 images in `MaintenanceAttachmentService.java` *(deferred — still photo_urls)*
- [x] T024 [US1] Implement `listForCustomer(customerId, status, pageable)` and `getDetailForCustomer(ticketId, customerId)` in `MaintenanceTicketService.java`
- [x] T025 [US1] Create `CustomerMaintenanceController.java` with `POST /api/v1/maintenance-tickets`, `GET /api/v1/maintenance-tickets/me`, `GET /api/v1/maintenance-tickets/{id}` in `Backend/src/main/java/com/homestay/controllers/CustomerMaintenanceController.java`
- [x] T026 [US1] Map `photoUrls`/`attachments` in detail response; hide `workNote` from Customer in `MaintenanceTicketService.java`
- [x] T027 [US1] Log `TICKET_CREATED` to ActivityLog in `MaintenanceTicketService.createTicket()` in `MaintenanceTicketService.java`
- [x] T028 [P] [US1] Migrate `createTicket` and `getCustomerTickets` to `/api/v1/maintenance-tickets` and `/me` in `Frontend/src/api/maintenanceApi.ts`
- [x] T029 [US1] Wire create form multipart (bookingId, title, description, files) on `Frontend/src/pages/customer/MaintenancePages.tsx` SCR-23
- [x] T030 [US1] Wire ticket list with status filter on `Frontend/src/pages/customer/MaintenancePages.tsx` SCR-22

**Checkpoint**: US1 MVP — Customer create + list testable

---

## Phase 4: User Story 2 — Customer chỉnh sửa hoặc xóa ticket khi Open (Priority: P1)

**Goal**: Customer PUT/DELETE only when status OPEN; soft-delete; attachment replace on edit

**Independent Test**: Open ticket → edit title/description → saved; Assigned ticket → edit/delete returns 409

### Implementation

- [x] T031 [US2] Implement `updateTicket(ticketId, customerId, title, description, keepAttachmentIds, newFiles)` — OPEN-only guard in `MaintenanceTicketService.java`
- [x] T032 [US2] Implement `softDeleteTicket(ticketId, customerId)` — set `deleted_at`, OPEN-only in `MaintenanceTicketService.java`
- [x] T033 [US2] Add `PUT /api/v1/maintenance-tickets/{id}` multipart and `DELETE /api/v1/maintenance-tickets/{id}` in `CustomerMaintenanceController.java`
- [ ] T034 [US2] Replace attachment set via `keepAttachmentIds` + new files in `MaintenanceAttachmentService.java`
- [x] T035 [US2] Log `TICKET_UPDATED` and `TICKET_DELETED` to ActivityLog in `MaintenanceTicketService.java`
- [x] T036 [P] [US2] Add `updateTicket` and `deleteTicket` v1 paths in `Frontend/src/api/maintenanceApi.ts`
- [x] T037 [US2] Wire edit/delete UI on Open tickets only in `Frontend/src/pages/customer/MaintenancePages.tsx` detail view

**Checkpoint**: US2 testable — Open-only edit/delete enforced

---

## Phase 5: User Story 3 — Manager xem danh sách và gán Employee (Priority: P1)

**Goal**: Manager property-scoped list; assign Employee same property; OPEN→ASSIGNED; SCR-41 Drawer

**Independent Test**: Manager P sees only P tickets → assign Employee E → ASSIGNED + assignedAt; cross-property assign rejected

### Implementation

- [x] T038 [US3] Implement `listForManager(managerId, propertyId, status, pageable)` with `PropertyScopeService` in `MaintenanceTicketService.java`
- [x] T039 [US3] Implement `assignEmployee(ticketId, managerId, assigneeId)` — same-property check, OPEN→ASSIGNED or reassign in `MaintenanceTicketStatusService.java`
- [x] T040 [US3] Validate assignee is EMPLOYEE with active property assignment (FR-06) in `MaintenanceTicketStatusService.java`
- [x] T041 [US3] Create `ManagerMaintenanceController.java` with `GET /api/v1/manager/maintenance-tickets`, `GET /{id}`, `PATCH /{id}/assign` in `Backend/src/main/java/com/homestay/controllers/ManagerMaintenanceController.java`
- [x] T042 [US3] Log `TICKET_ASSIGNED` to ActivityLog in `MaintenanceTicketStatusService.java`
- [ ] T043 [US3] Call `MaintenanceNotificationPublisher.publishStatusChanged` on assign in `MaintenanceTicketStatusService.java`
- [ ] T044 [P] [US3] Migrate manager list to `GET /api/v1/manager/maintenance-tickets?propertyId=` and add `assignTicket` PATCH in `Frontend/src/api/maintenanceApi.ts`
- [ ] T045 [US3] Wire property filter + status table on `Frontend/src/pages/manager/MaintenanceMgmtListPage.tsx`
- [ ] T046 [US3] Add assign Employee Drawer using `GET /api/v1/manager/employees?propertyId=` on `Frontend/src/pages/manager/MaintenanceMgmtListPage.tsx`

**Checkpoint**: US3 testable — manager assign SCR-41 complete

---

## Phase 6: User Story 4 — Employee thực hiện bảo trì và cập nhật tiến độ (Priority: P1)

**Goal**: Employee sees assigned tickets only; ASSIGNED→IN_PROGRESS→RESOLVED; optional workNote; SCR-61 NEW

**Independent Test**: Employee E updates assigned ticket → IN_PROGRESS → RESOLVED; non-assignee and invalid skip rejected

### Implementation

- [x] T047 [US4] Implement `listForEmployee(employeeId, status, pageable)` — `assigned_employee_id = employeeId` in `MaintenanceTicketService.java`
- [x] T048 [US4] Implement `updateStatusByEmployee(ticketId, employeeId, status, workNote)` — ASSIGNED→IN_PROGRESS, IN_PROGRESS→RESOLVED in `MaintenanceTicketStatusService.java`
- [x] T049 [US4] Reject non-assignee and invalid transitions in `MaintenanceTicketStatusService.java`
- [x] T050 [US4] Create `EmployeeMaintenanceController.java` with `GET /api/v1/employee/maintenance-tickets`, `PATCH /{id}/status` in `Backend/src/main/java/com/homestay/controllers/EmployeeMaintenanceController.java`
- [x] T051 [US4] Log `TICKET_STATUS_CHANGED` and publish notification on employee transitions in `MaintenanceTicketStatusService.java`
- [x] T053 [US4] Create `Frontend/src/pages/employee/MaintenanceWorkspacePage.tsx` — list + Start/Finish actions SCR-61
- [x] T054 [US4] Register `/employee/maintenance` route with Employee role guard in `Frontend/src/App.tsx` (add Employee layout if missing)

**Checkpoint**: US4 testable — employee workspace SCR-61 complete

---

## Phase 7: User Story 5 — Manager xác nhận hoàn thành và đóng ticket (Priority: P1)

**Goal**: Manager closes RESOLVED tickets with mandatory resolutionNote; RESOLVED→CLOSED; SCR-41 verify

**Independent Test**: Resolved ticket → PATCH close with note → CLOSED + verifiedBy/At; close without note or wrong status rejected

### Implementation

- [x] T055 [US5] Implement `closeTicket(ticketId, managerId, resolutionNote)` — RESOLVED→CLOSED, min 10 chars in `MaintenanceTicketStatusService.java`
- [x] T057 [US5] Log `TICKET_CLOSED` and publish notification in `MaintenanceTicketStatusService.java`
- [ ] T058 [US5] Refactor `Frontend/src/pages/manager/MaintenanceMgmtDetailPage.tsx` — remove Manager direct status dropdown; add close form when RESOLVED only
- [ ] T059 [US5] Show customer photos + workNote (Manager view) on `Frontend/src/pages/manager/MaintenanceMgmtDetailPage.tsx`

**Checkpoint**: US5 testable — full lifecycle through CLOSED

---

## Phase 8: User Story 6 — Customer theo dõi tiến độ và nhận thông báo (Priority: P2)

**Goal**: Notification events on status changes; Admin read-only global list; Customer sees resolution note when Closed

**Independent Test**: Each transition emits event/outbox stub; Admin GET all tickets read-only; Customer detail shows resolution note on CLOSED

### Implementation

- [ ] T060 [US6] Complete `MaintenanceNotificationPublisher.publishStatusChanged` — Outbox stub `MAINTENANCE_STATUS_CHANGED` per `data-model.md` in `MaintenanceNotificationPublisher.java`
- [x] T061 [US6] Emit notifications on ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED (not OPEN create) in `MaintenanceTicketStatusService.java`
- [x] T062 [US6] Create `AdminMaintenanceController.java` with read-only `GET /api/v1/admin/maintenance-tickets` and `GET /{id}` in `Backend/src/main/java/com/homestay/controllers/AdminMaintenanceController.java`
- [ ] T063 [US6] Show `resolutionNote` on Customer detail when status CLOSED in `MaintenanceTicketService.getDetailForCustomer()` in `MaintenanceTicketService.java`
- [ ] T064 [US6] Add status timeline/badges for all 5 states on `Frontend/src/pages/customer/MaintenancePages.tsx` detail view
- [ ] T065 [P] [US6] Align `StatusBadge` colors for OPEN/ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED in `Frontend/src/pages/manager/MaintenanceMgmtListPage.tsx` and customer list

**Checkpoint**: US6 testable — notification stub + admin read + customer closure UX

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Tests, dedupe pages, quickstart validation, security hardening

- [ ] T066 [P] Unit test valid/invalid status transitions in `Backend/src/test/java/com/homestay/unit/MaintenanceTicketStatusServiceTest.java`
- [ ] T067 [P] Unit test active booking gate in `Backend/src/test/java/com/homestay/unit/MaintenanceBookingValidatorTest.java`
- [ ] T068 Integration test Customer create + Manager assign + Employee progress + Manager close RBAC in `Backend/src/test/java/com/homestay/integration/MaintenanceControllerIT.java`
- [ ] T069 [P] Assert Open-only edit/delete and property scope 403 in `MaintenanceControllerIT.java`
- [x] T070 [P] Remove or redirect duplicate routes `MaintenanceListPage.tsx`, `CreateMaintenancePage.tsx`, `MaintenanceDetailPage.tsx` in `Frontend/src/App.tsx` — canonical `MaintenancePages.tsx`
- [ ] T071 Run curl smoke tests in `specs/013-maintenance-management/quickstart.md` and fix gaps
- [ ] T072 [P] Verify all maintenance routes in `Frontend/src/App.tsx` (customer/manager/employee)
- [ ] T073 Document FR-15 integration handoff comment in `MaintenanceNotificationPublisher.java`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: After FR-04 bookings (blocker)
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **US1 (P1)**: After Foundational — **MVP**; blocks US2 (edit needs create)
- **US2 (P1)**: After US1 (shared CustomerMaintenanceController)
- **US3 (P1)**: After US1 (needs OPEN tickets to assign)
- **US4 (P1)**: After US3 (needs ASSIGNED tickets)
- **US5 (P1)**: After US4 (needs RESOLVED tickets)
- **US6 (P2)**: After US3–US5 (notification on transitions); Admin list independent after Foundational
- **Polish (Phase 9)**: After desired user stories complete

### User Story Dependencies

```text
Foundational → US1 → US2
                    → US3 → US4 → US5
                    → US6 (parallel after US5 for full notification path)
```

### Parallel Opportunities

**Phase 2** (after T002): T006–T012 all [P]  
**US1**: T028 parallel with backend if API contract fixed first  
**US3**: T044 parallel with T038–T043  
**US4**: T052 parallel with T047–T051  
**Polish**: T066, T067, T069, T070, T072 parallel

### Parallel Example: User Story 1

```bash
# Backend create flow:
T021 MaintenanceBookingValidator.java
T022–T027 MaintenanceTicketService + Controller

# Frontend (after T025 contract stable):
T028 maintenanceApi.ts
T029–T030 MaintenancePages.tsx
```

### Parallel Example: User Story 4

```bash
T052 maintenanceApi.ts employee methods
T053 MaintenanceWorkspacePage.tsx
T054 App.tsx route
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T005)
2. Complete Phase 2: Foundational (T006–T020)
3. Complete Phase 3: User Story 1 (T021–T030)
4. **STOP and VALIDATE**: Customer create + list per `quickstart.md` steps 1–2
5. Demo SCR-22/23

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 → Customer create (MVP)
3. US2 → Edit/delete Open
4. US3 → Manager assign
5. US4 → Employee progress
6. US5 → Manager close (full lifecycle)
7. US6 → Notifications + Admin read
8. Polish → Tests + quickstart

### Parallel Team Strategy

1. Team completes Setup + Foundational together
2. After Foundational:
   - Dev A: US1 + US2 (Customer path)
   - Dev B: US3 + US5 (Manager path) — after US1 seed data
   - Dev C: US4 (Employee SCR-61) — after US3
3. US6 + Polish when core lifecycle works

---

## Notes

- FR-08 Room status **Maintenance** (manual lock) — **out of scope**; do not couple ticket workflow to room calendar
- Manager **must not** skip Employee steps — refactor `MaintenanceMgmtDetailPage.tsx` in US5
- `attachments` table may exist from FR-23 — V029 uses `IF NOT EXISTS` pattern per `research.md` §13
- FR-15 stub acceptable v1 — ticket workflow works without push
- Status enum: API uses `ASSIGNED` (not skip to `IN_PROGRESS` from OPEN)
- Commit after each task or logical group; stop at any checkpoint to validate story independently
