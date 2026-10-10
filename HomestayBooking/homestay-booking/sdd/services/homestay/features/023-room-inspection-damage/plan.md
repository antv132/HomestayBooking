# Implementation Plan: FR-23 Room Inspection & Damage Resolution

**Branch**: `025-room-inspection-damage` | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/023-room-inspection-damage/spec.md`

**Nguồn docs**: `docs/Specification_v2.md` (FR-23, §5 RoomInspection/DamageReport/DamageItem, §6 RBAC, §10 Room Inspection), `docs/api-spec-by-screen.md` (SCR-42/43/53/62/63/64), `docs/screen.md`, `docs/screendesign.md`, `docs/entity-ui-mapping.md`

**Phụ thuộc**: FR-01 (auth); FR-04 (booking checkout + PENDING_INSPECTION); FR-06 (Manager scope); FR-08 (Room); FR-12 (Damage Fee payment); FR-15 (notifications); FR-20 (Employee property); FR-21 (housekeeping after checkout); FR-22 (dashboard read-only). **Ranh giới**: FR-23 owns inspection/damage entities + SCR-42/43/53/62/63/64; FR-04 owns checkout UI; FR-12 owns payment gateway; FR-10 Contract Addendum **P2**.

## Summary

Triển khai **FR-23 Room Inspection & Damage Resolution**: Flyway **V039** mở rộng `room_inspections` + bảng `damage_reports`, `damage_items`; `RoomInspectionService` + `DamageReportService`; checkout gate `InspectionCheckoutGateService`; Employee SCR-62/63/64; Manager SCR-42/43 approve + escalate; Admin SCR-53 co-approve; Customer Dispute 24h; P2 Outstanding Debt flag; tích hợp FR-04 checkout + FR-12 Damage Fee payment + FR-15 events.

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA, Bean Validation; Vite, Axios, React Router  
**Storage**: SQL Server — `room_inspections` (expand V039), `damage_reports`, `damage_items`; reuse `attachments` (FR-13); update `bookings.damage_fee_amount`; optional `users.outstanding_debt`  
**Testing**: JUnit 5 + Mockito; `RoomInspectionServiceTest`, `DamageReportServiceTest`; `InspectionDamageControllerIT` RBAC + checkout gate + escalation  
**Target Platform**: Web application (mobile-first Employee SCR-62/64)  
**Project Type**: `Frontend/` + `Backend/`  
**Performance Goals**: Inspection submit p95 < 2s; Manager approve drawer < 3s  
**Constraints**: One inspection per booking; 5M VND escalation threshold; 24h dispute window Asia/Ho_Chi_Minh; property scope  
**Scale/Scope**: ~15 REST endpoints; 7 user stories; 6 screens

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Layered architecture | PASS | Controllers → RoomInspectionService / DamageReportService → repos |
| DTO + Bean Validation | PASS | Inspection/Damage request DTOs validated |
| Security-first (RBAC) | PASS | EMPLOYEE create; MANAGER approve; ADMIN co-approve; CUSTOMER dispute |
| No secrets in code | PASS | Escalation threshold via config property |
| Test coverage ≥80% | PASS | Gate + escalation + dispute IT |
| Standard API envelope | PASS | `{ success, message, data }` |
| Audit log | PASS | INSPECTION_*, DAMAGE_* via ActivityLogService |
| Pagination | PASS | Manager/Employee lists paginated |

**Post-design re-check**: PASS — checkout gate centralized; FR-12 payment creation on approve documented.

## Project Structure

### Documentation (this feature)

```text
specs/023-room-inspection-damage/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/inspection-damage-api.yaml
├── spec.md
└── tasks.md              # /speckit-tasks (next)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controllers/
│   ├── EmployeeRoomInspectionController.java   # SCR-62
│   ├── EmployeeDamageReportController.java     # SCR-63/64
│   ├── ManagerRoomInspectionController.java    # SCR-42
│   ├── ManagerDamageReportController.java      # SCR-43 + P2 outstanding debt
│   ├── AdminDamageReportController.java        # SCR-53
│   └── CustomerDamageReportController.java     # Dispute
├── dtos/inspection/
│   ├── RoomInspectionResponse.java
│   ├── SubmitInspectionRequest.java
│   └── InspectionChecklistDto.java
├── dtos/damage/
│   ├── DamageReportResponse.java
│   ├── CreateDamageReportRequest.java
│   ├── DamageItemDto.java
│   ├── ApproveDamageReportRequest.java
│   ├── CoApproveDamageReportRequest.java
│   └── DisputeDamageReportRequest.java
├── entities/
│   ├── RoomInspection.java
│   ├── DamageReport.java
│   └── DamageItem.java
├── enums/
│   ├── RoomInspectionStatus.java
│   └── DamageReportStatus.java
├── repositories/
│   ├── RoomInspectionRepository.java
│   ├── DamageReportRepository.java
│   └── DamageItemRepository.java
├── services/
│   ├── RoomInspectionService.java
│   ├── DamageReportService.java
│   └── InspectionCheckoutGateService.java      # FR-04 hook
└── configs/
    ├── SecurityConfig.java
    └── DamageEscalationProperties.java         # threshold 5M default

Backend/src/main/resources/db/migration/
└── V039__room_inspection_damage_fr23.sql

Backend/src/test/java/com/homestay/
├── unit/RoomInspectionServiceTest.java
├── unit/DamageReportServiceTest.java
└── integration/InspectionDamageControllerIT.java

Frontend/src/
├── api/
│   ├── roomInspectionApi.ts
│   └── damageReportApi.ts
├── pages/employee/
│   ├── RoomInspectionHubPage.tsx               # SCR-62
│   ├── DamageReportListPage.tsx                # SCR-63
│   └── CreateDamageReportPage.tsx              # SCR-64
├── pages/manager/
│   ├── InspectionManagementPage.tsx          # SCR-42
│   └── DamageReportManagementPage.tsx          # SCR-43
├── pages/admin/
│   └── DamageEscalationPage.tsx                # SCR-53
├── components/inspection/
│   └── InspectionChecklistDrawer.tsx
├── components/damage/
│   └── DamageReportDetailDrawer.tsx
└── App.tsx                                     # routes per screen
```

**Structure Decision**: FR-23 **owns** inspection/damage tables (V039). **Hooks** FR-04 `requestCheckout` → create inspection; `InspectionCheckoutGateService` before `completeCheckout`. **Delegates** payment creation to FR-12 on approve.

## Implementation Phases

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | V039 migration + enums + entities | data-model.md |
| **B** | RoomInspectionService + checkout gate | US1, US3 |
| **C** | DamageReportService create/list | US2 |
| **D** | Manager approve + escalation | US4 |
| **E** | Admin co-approve | US5 |
| **F** | Customer dispute | US6 |
| **G** | Outstanding debt P2 | US7 |
| **H** | Frontend SCR-62–64, 42/43, 53 | All |
| **I** | Tests + quickstart + FR-15 events | SC-001–SC-007 |

## Frontend Gap Analysis

| Item | Hiện tại | Target |
|------|----------|--------|
| SCR-62 | **missing** | `RoomInspectionHubPage.tsx` |
| SCR-63/64 | **missing** | `DamageReportListPage.tsx`, `CreateDamageReportPage.tsx` |
| SCR-42/43 | **missing** | Manager inspection + damage pages |
| SCR-53 | **missing** | `DamageEscalationPage.tsx` |
| API clients | **missing** | `roomInspectionApi.ts`, `damageReportApi.ts` |
| Customer Dispute UI | **partial** (FR-19 P2 banner) | dispute action on notification/dashboard |

## Risks

| Risk | Mitigation |
|------|------------|
| FR-04 stub inspection only | V039 expands table; gate service replaces stub `isPassed` |
| FR-12 payment API missing | Stub Damage Fee payment record; integration test |
| Concurrent approve/dispute | Optimistic `@Version` or status guard in service |
| FR-21 depends on checkout | Document hook order: gate → checkout → FR-21 |
| Admin dispute resolution P2 | v1 Dispute blocks payment; Admin notified only |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/inspection-damage-api.yaml](./contracts/inspection-damage-api.yaml)
- [quickstart.md](./quickstart.md)

## Next Step

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md`.
