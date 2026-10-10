# Tasks: FR-23 Room Inspection & Damage Resolution

**Input**: Design documents from `specs/023-room-inspection-damage/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/inspection-damage-api.yaml, quickstart.md

**Phụ thuộc**: FR-01 (auth all roles); FR-04 (booking checkout + PENDING_INSPECTION); FR-06/FR-20 (property scope); FR-08 (Room); FR-12 (DAMAGE_FEE payment); FR-15 (notifications); FR-21 (post-checkout hook). **Ranh giới**: FR-23 owns inspection/damage entities + SCR-42/43/53/62/63/64; FR-04 checkout UI; FR-12 payment gateway; FR-10 Contract Addendum P2.

**Tests**: Không có phase test riêng per-story. Unit + integration trong Phase Polish per plan Phase I.

**Organization**: Tasks grouped by user story (US1–US7) for independent delivery.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no blocking deps)
- **[Story]**: US1–US7 maps to spec.md user stories

## Path Conventions

- **Backend**: `Backend/src/main/java/com/homestay/`
- **Backend tests**: `Backend/src/test/java/com/homestay/`
- **Frontend**: `Frontend/src/`
- **Migrations**: `Backend/src/main/resources/db/migration/`
- **Contract**: `specs/023-room-inspection-damage/contracts/inspection-damage-api.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Blockers, V039 migration, dev proxy

- [ ] T001 Verify FR-01 JWT auth with **EMPLOYEE**, **MANAGER**, **ADMIN**, **CUSTOMER** roles per `specs/001-user-auth/quickstart.md` (blocker)
- [X] T002 Verify FR-04 booking CHECKED_IN + `requestCheckout` flow and FR-06/FR-20 property scope per respective quickstarts (blocker)
- [ ] T003 Create Flyway `Backend/src/main/resources/db/migration/V039__room_inspection_damage_fr23.sql` — expand `room_inspections`, `damage_reports`, `damage_items`, indexes per `data-model.md`
- [ ] T004 [P] Add `bookings.damage_fee_amount` and `users.outstanding_debt` columns in V039 if missing per `data-model.md`
- [ ] T005 [P] Confirm Vite proxy `/api/v1` → backend in `Frontend/vite.config.ts`
- [ ] T006 [P] Add dev seed SQL comments in V039 — checked-in booking, inspection PENDING, sample damage report for local demo

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Entities, enums, repositories, DTOs, service skeletons, config, security — MUST complete before user story phases

**⚠️ CRITICAL**: No user story work until this phase complete

- [X] T007 [P] Create `RoomInspectionStatus.java` enum in `Backend/src/main/java/com/homestay/enums/RoomInspectionStatus.java` — PENDING, IN_PROGRESS, PASSED, FAILED_WITH_DAMAGE
- [X] T008 [P] Create `DamageReportStatus.java` enum in `Backend/src/main/java/com/homestay/enums/DamageReportStatus.java` — PENDING_APPROVAL, ESCALATED, APPROVED, DISPUTED, PAID
- [ ] T009 [P] Create `RoomInspection.java` entity in `Backend/src/main/java/com/homestay/entities/RoomInspection.java` per `data-model.md`
- [ ] T010 [P] Create `DamageReport.java` entity in `Backend/src/main/java/com/homestay/entities/DamageReport.java`
- [ ] T011 [P] Create `DamageItem.java` entity in `Backend/src/main/java/com/homestay/entities/DamageItem.java`
- [ ] T012 [P] Create `RoomInspectionRepository.java` in `Backend/src/main/java/com/homestay/repositories/RoomInspectionRepository.java` — `findByBookingId`, `findByPropertyIdAndStatus`, `findByInspectedByAndStatus`
- [ ] T013 [P] Create `DamageReportRepository.java` in `Backend/src/main/java/com/homestay/repositories/DamageReportRepository.java`
- [ ] T014 [P] Create `DamageItemRepository.java` in `Backend/src/main/java/com/homestay/repositories/DamageItemRepository.java`
- [ ] T015 [P] Create inspection DTOs in `Backend/src/main/java/com/homestay/dtos/inspection/` — `RoomInspectionResponse.java`, `SubmitInspectionRequest.java`, `InspectionChecklistDto.java`
- [X] T016 [P] Create damage DTOs in `Backend/src/main/java/com/homestay/dtos/damage/` — `DamageReportResponse.java`, `CreateDamageReportRequest.java`, `DamageItemDto.java`, `ApproveDamageReportRequest.java`, `CoApproveDamageReportRequest.java`, `DisputeDamageReportRequest.java`
- [ ] T017 Create `DamageEscalationProperties.java` in `Backend/src/main/java/com/homestay/configs/DamageEscalationProperties.java` — `escalationThresholdVnd` default 5_000_000
- [ ] T018 Create `RoomInspectionService.java` skeleton in `Backend/src/main/java/com/homestay/services/RoomInspectionService.java`
- [ ] T019 Create `DamageReportService.java` skeleton in `Backend/src/main/java/com/homestay/services/DamageReportService.java`
- [X] T020 Create `InspectionCheckoutGateService.java` skeleton in `Backend/src/main/java/com/homestay/services/InspectionCheckoutGateService.java`
- [ ] T021 Register RBAC paths in `Backend/src/main/java/com/homestay/configs/SecurityConfig.java` — employee/manager/admin/customer inspection-damage routes
- [ ] T022 [P] Create `roomInspectionApi.ts` skeleton in `Frontend/src/api/roomInspectionApi.ts`
- [ ] T023 [P] Create `damageReportApi.ts` skeleton in `Frontend/src/api/damageReportApi.ts`

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Employee thực hiện Room Inspection (Priority: P1) 🎯 MVP

**Goal**: SCR-62 checklist Pass/Fail; inspection status transitions

**Independent Test**: Employee start + submit PASS → inspection PASSED; FAIL → FAILED_WITH_DAMAGE; property scope enforced

### Implementation

- [ ] T024 [US1] Implement `createForBooking(bookingId)` idempotent + `listForEmployee(employeeId)` in `RoomInspectionService.java`
- [ ] T025 [US1] Implement `startInspection(id, employeeId)` → IN_PROGRESS + set `inspectedBy` in `RoomInspectionService.java`
- [X] T026 [US1] Implement `submitInspection(id, result, checklist, note, employeeId)` → PASSED | FAILED_WITH_DAMAGE in `RoomInspectionService.java`
- [ ] T027 [US1] Create `EmployeeRoomInspectionController.java` — `GET/POST /api/v1/employee/room-inspections`, `PATCH .../submit` in `Backend/src/main/java/com/homestay/controllers/EmployeeRoomInspectionController.java`
- [ ] T028 [P] [US1] Implement `listInspections()`, `startInspection()`, `submitInspection()` in `Frontend/src/api/roomInspectionApi.ts`
- [ ] T029 [US1] Create `InspectionChecklistDrawer.tsx` with TV/Minibar/Bed/Bathroom checkboxes in `Frontend/src/components/inspection/InspectionChecklistDrawer.tsx`
- [X] T030 [US1] Create `RoomInspectionHubPage.tsx` mobile list + Pass/Fail actions in `Frontend/src/pages/employee/RoomInspectionHubPage.tsx`
- [X] T031 [US1] Wire Fail → navigate to `/employee/damage-reports/new?inspectionId=` on `RoomInspectionHubPage.tsx`
- [X] T032 [US1] Register `/employee/inspections` with `ProtectedRoute role="EMPLOYEE"` in `Frontend/src/App.tsx`

**Checkpoint**: US1 MVP — Employee inspection Pass/Fail testable via `quickstart.md`

---

## Phase 4: User Story 2 — Employee ghi nhận Damage Report (Priority: P1)

**Goal**: Create DamageReport + DamageItems + attachments (SCR-63/64)

**Independent Test**: After FAIL inspection → create report with 2 items → PENDING_APPROVAL; list on SCR-63

### Implementation

- [X] T033 [US2] Implement `createReport(request, employeeId)` — validate FAILED_WITH_DAMAGE inspection, sum items in `DamageReportService.java`
- [X] T034 [US2] Wire Attachment save for DamageItem photos (entity_type DAMAGE_ITEM) in `DamageReportService.java`
- [ ] T035 [US2] Implement `listForEmployee(employeeId)` in `DamageReportService.java`
- [ ] T036 [US2] Create `EmployeeDamageReportController.java` — `GET/POST /api/v1/employee/damage-reports` in `Backend/src/main/java/com/homestay/controllers/EmployeeDamageReportController.java`
- [ ] T037 [P] [US2] Implement `listReports()` and `createReport()` in `Frontend/src/api/damageReportApi.ts`
- [X] T038 [US2] Create `CreateDamageReportPage.tsx` dynamic item list + photo upload in `Frontend/src/pages/employee/CreateDamageReportPage.tsx`
- [ ] T039 [US2] Create `DamageReportListPage.tsx` (SCR-63) with FAB → create in `Frontend/src/pages/employee/DamageReportListPage.tsx`
- [X] T040 [US2] Register `/employee/damage-reports` and `/employee/damage-reports/new` in `Frontend/src/App.tsx`

**Checkpoint**: US2 testable — damage report create + list

---

## Phase 5: User Story 3 — Check-out gate (Priority: P1)

**Goal**: Block checkout until inspection complete + damage fee paid; FR-04 integration

**Independent Test**: Checkout without PASSED inspection → 409; after PASSED + paid → checkout succeeds

### Implementation

- [X] T041 [US3] Implement `assertCanCheckout(bookingId)` with gate rules per `data-model.md` in `InspectionCheckoutGateService.java`
- [X] T042 [US3] Wire `RoomInspectionService.createForBooking` from FR-04 `BookingService.requestCheckout()` in `BookingService.java`
- [X] T043 [US3] Call `InspectionCheckoutGateService.assertCanCheckout` before `completeCheckout` in `BookingService.java`
- [X] T044 [US3] Return error codes `INSPECTION_REQUIRED`, `DAMAGE_REPORT_REQUIRED`, `DAMAGE_FEE_UNPAID` from gate service
- [X] T045 [US3] Update booking status transitions PENDING_INSPECTION / PENDING_DAMAGE_PAYMENT per FR-04 state machine in `BookingService.java`

**Checkpoint**: US3 testable — checkout blocked/allowed per quickstart smoke tests

---

## Phase 6: User Story 4 — Manager phê duyệt Damage Fee (Priority: P1)

**Goal**: SCR-42 inspection list + SCR-43 approve with escalation > 5M

**Independent Test**: Manager approve 4M → APPROVED + balance update; approve 6M → ESCALATED

### Implementation

- [ ] T046 [US4] Implement `listForManager(propertyId, managerId)` in `RoomInspectionService.java`
- [ ] T047 [US4] Implement `listForManager(propertyId, managerId)` in `DamageReportService.java`
- [X] T048 [US4] Implement `approve(reportId, fee, note, managerId)` — APPROVED vs ESCALATED threshold in `DamageReportService.java`
- [X] T049 [US4] On APPROVED: set `booking.damageFeeAmount`, update remaining balance, call FR-12 `createPendingDamageFee` in `DamageReportService.java`
- [ ] T050 [US4] Create `ManagerRoomInspectionController.java` — `GET /api/v1/manager/room-inspections` in `Backend/src/main/java/com/homestay/controllers/ManagerRoomInspectionController.java`
- [X] T051 [US4] Create `ManagerDamageReportController.java` — `GET .../damage-reports`, `PATCH .../approve` in `Backend/src/main/java/com/homestay/controllers/ManagerDamageReportController.java`
- [X] T052 [US4] Create `DamageReportDetailDrawer.tsx` with photos + approve form in `Frontend/src/components/damage/DamageReportDetailDrawer.tsx`
- [X] T053 [US4] Create `InspectionManagementPage.tsx` (SCR-42) in `Frontend/src/pages/manager/InspectionManagementPage.tsx`
- [X] T054 [US4] Create `DamageReportManagementPage.tsx` (SCR-43) with escalate message when fee > 5M in `Frontend/src/pages/manager/DamageReportManagementPage.tsx`
- [X] T055 [US4] Register `/manager/inspections` and `/manager/damage-reports` in `Frontend/src/App.tsx`

**Checkpoint**: US4 testable — Manager approve and escalation

---

## Phase 7: User Story 5 — Admin co-approve escalated (Priority: P1)

**Goal**: SCR-53 co-approve ESCALATED reports

**Independent Test**: ESCALATED 6M → Admin co-approve → APPROVED + damage fee applied

### Implementation

- [X] T056 [US5] Implement `coApprove(reportId, approvedFee, note, adminId)` in `DamageReportService.java`
- [X] T057 [US5] Create `AdminDamageReportController.java` — `GET /api/v1/admin/damage-reports`, `PATCH .../co-approve` in `Backend/src/main/java/com/homestay/controllers/AdminDamageReportController.java`
- [X] T058 [P] [US5] Add `listEscalated()` and `coApprove()` to `Frontend/src/api/damageReportApi.ts`
- [ ] T059 [US5] Create `DamageEscalationPage.tsx` (SCR-53) in `Frontend/src/pages/admin/DamageEscalationPage.tsx`
- [ ] T060 [US5] Register `/admin/damage-escalation` with `ProtectedRoute role="ADMIN"` in `Frontend/src/App.tsx`

**Checkpoint**: US5 testable — Admin co-approve flow

---

## Phase 8: User Story 6 — Customer Dispute 24h (Priority: P1)

**Goal**: Customer disputes approved damage within 24h; escalate Admin

**Independent Test**: Approved report → Customer dispute within 24h → DISPUTED; after 24h → rejected

### Implementation

- [X] T061 [US6] Implement `dispute(reportId, note, customerId)` with 24h window Asia/Ho_Chi_Minh in `DamageReportService.java`
- [X] T062 [US6] Block damage payment when status DISPUTED in `DamageReportService.java`
- [ ] T063 [US6] Create `CustomerDamageReportController.java` — `GET/PATCH /api/v1/customer/damage-reports` in `Backend/src/main/java/com/homestay/controllers/CustomerDamageReportController.java`
- [X] T064 [P] [US6] Add `dispute(id, note)` to `Frontend/src/api/damageReportApi.ts`
- [X] T065 [US6] Add Dispute action on customer notification or booking detail (wire existing customer pages) in `Frontend/src/pages/customer/`
- [X] T066 [US6] Emit FR-15 `DAMAGE_REPORT_DISPUTED` notification to Admin on dispute in `DamageReportService.java`

**Checkpoint**: US6 testable — dispute within 24h window

---

## Phase 9: User Story 7 — Outstanding Debt (Priority: P2)

**Goal**: Manager marks Outstanding Debt; block new bookings

**Independent Test**: Approved unpaid damage → Manager mark debt → Customer cannot create booking

### Implementation

- [ ] T067 [US7] Implement `markOutstandingDebt(reportId, reason, managerId)` in `DamageReportService.java`
- [ ] T068 [US7] Add `PATCH /api/v1/manager/damage-reports/{id}/mark-outstanding-debt` in `ManagerDamageReportController.java`
- [ ] T069 [US7] Wire `users.outstanding_debt = true` for booking customer in `DamageReportService.java`
- [ ] T070 [US7] Add FR-04 booking create guard for `outstandingDebt` customers in `BookingService.java`
- [ ] T071 [US7] Clear `outstanding_debt` on DAMAGE_FEE payment Completed in `PaymentService.java` or damage listener

**Checkpoint**: US7 testable — outstanding debt blocks booking

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Tests, notifications, quickstart, audit

- [ ] T072 [P] Unit test inspection transitions in `Backend/src/test/java/com/homestay/unit/RoomInspectionServiceTest.java`
- [X] T073 [P] Unit test approve/escalate/dispute in `Backend/src/test/java/com/homestay/unit/DamageReportServiceTest.java`
- [X] T074 Integration test RBAC + checkout gate + escalation in `Backend/src/test/java/com/homestay/integration/InspectionDamageControllerIT.java`
- [X] T075 [P] Assert cross-property Manager 403 and dispute window expiry in `InspectionDamageControllerIT.java`
- [ ] T076 Run curl smoke tests in `specs/023-room-inspection-damage/quickstart.md` and fix gaps
- [ ] T077 [P] Log `INSPECTION_*`, `DAMAGE_*` events via ActivityLogService in inspection/damage services
- [X] T078 [P] Emit FR-15 notifications on report submitted, approved, escalated in `DamageReportService.java`
- [ ] T079 [P] Add Manager/Employee nav links for inspections and damage reports in respective layouts

---


---

## Closeout (2026-07-23 align-to-audit)

- [X] C001 Register /manager/inspections + /manager/damage-reports in App.tsx (SCR-42/43)
- [X] C002 Employee damage photo upload via multipart POST .../damage-reports/photos (no blob URLs)
- [X] C003 Spec aliases /employee/damage-reports → /employee/damage

Deferred P2: Outstanding Debt (FR-014/015); escalation by approvedAmount vs estimated; DamageItem.description wire.


## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: After FR-01, FR-04, FR-06, FR-20 blockers
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **US1 (P1)**: After Foundational — **MVP** inspection
- **US2 (P1)**: After US1 (needs FAILED_WITH_DAMAGE)
- **US3 (P1)**: After US1; integrates with FR-04
- **US4 (P1)**: After US2 (needs damage reports)
- **US5 (P1)**: After US4 (needs ESCALATED)
- **US6 (P1)**: After US4 (needs APPROVED)
- **US7 (P2)**: After US4
- **Polish (Phase 10)**: After desired stories

### User Story Dependencies

```text
Foundational → US1 (Employee inspection) — MVP
            → US2 (Damage report create) — needs US1 FAIL
            → US3 (Checkout gate) — needs US1; FR-04 hook
            → US4 (Manager approve) — needs US2
            → US5 (Admin co-approve) — needs US4 escalation
            → US6 (Customer dispute) — needs US4/5 APPROVED
            → US7 (Outstanding debt) — P2 after US4
```

### Parallel Opportunities

**Phase 1**: T004, T005, T006 [P]  
**Phase 2**: T007–T016, T022, T023 all [P]  
**US1**: T028 parallel with T024–T027  
**US4 backend**: T046–T051 parallel with T052–T055 frontend after APIs  
**Polish**: T072, T073, T075, T077, T078, T079 all [P]

### Parallel Example: User Story 1

```bash
T024–T027 Backend RoomInspectionService + EmployeeRoomInspectionController
T028 roomInspectionApi.ts (parallel)
T029–T032 InspectionChecklistDrawer + RoomInspectionHubPage + route
```

### Parallel Example: Foundational

```bash
T007–T011 Enums + entities (parallel)
T012–T014 Repositories (parallel)
T015–T016 DTOs (parallel)
T017–T021 Config + service skeletons + security
T022–T023 API client skeletons (parallel)
```

---

## Implementation Strategy

### MVP First (User Story 1 + US3 gate)

1. Complete Phase 1: Setup (T001–T006)
2. Complete Phase 2: Foundational (T007–T023)
3. Complete Phase 3: User Story 1 (T024–T032) — Employee inspection
4. Complete Phase 5: User Story 3 (T041–T045) — Checkout gate
5. **STOP and VALIDATE**: Pass inspection → checkout allowed (no damage path)

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 → Employee inspection Pass/Fail (MVP)
3. US3 → Checkout gate (FR-04 integration)
4. US2 → Damage report create
5. US4 → Manager approve + escalation
6. US5 → Admin co-approve
7. US6 → Customer dispute
8. US7 → Outstanding debt P2
9. Polish → Tests + quickstart + notifications

### Parallel Team Strategy

With multiple developers after Foundational:

- **Developer A**: US1 + US3 (inspection + checkout gate)
- **Developer B**: US2 + US4 (damage create + Manager approve)
- **Developer C**: US5 + US6 + US7 (Admin + Customer + debt P2)

---

## Notes

- V039 expands FR-04 stub `room_inspections` — do not create duplicate table
- Escalation threshold via `DamageEscalationProperties`; default 5_000_000 VND
- FR-12 `createPendingDamageFee` may be stub until payment feature complete
- FR-21 housekeeping runs only after successful checkout post-gate
- Admin dispute resolution UI minimal v1 — Dispute blocks payment only
- One inspection per booking — enforce unique `booking_id` in V039
