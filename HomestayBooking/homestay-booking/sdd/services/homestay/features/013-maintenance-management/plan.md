# Implementation Plan: FR-13 Maintenance Management

**Branch**: `015-maintenance-management` | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/013-maintenance-management/spec.md`

**Nguồn docs**: `docs/Specification_v2.md` (FR-13, §5 MaintenanceTicket/Attachment, §6 RBAC, §7 Validation, §8 Acceptance Maintenance), `docs/api-spec-by-screen.md` (SCR-22/23/41/61), `docs/screen.md`, `docs/screendesign.md`, `docs/entity-ui-mapping.md` §1.9 §2.3, frontend `maintenanceApi.ts`, `MaintenancePages.tsx`, `MaintenanceMgmtListPage.tsx`, `MaintenanceMgmtDetailPage.tsx`

**Phụ thuộc**: FR-04 (active booking validation); FR-06 (Manager/Employee property scope, `GET /manager/employees` for assign dropdown); FR-08 (Room → propertyId); FR-01 (auth JWT); FR-15 (notification delivery — FR-13 emits events only). **Ranh giới**: FR-15 owns notification engine/WebSocket; FR-08 Room calendar status **Maintenance** (manual lock SCR-33) is separate from MaintenanceTicket workflow.

## Summary

Triển khai **FR-13 Maintenance Management**: bảng `maintenance_tickets` + shared `attachments` (EntityType MAINTENANCE); Customer tạo/sửa/xóa ticket gắn active booking + ảnh (SCR-22/23); Manager xem theo Property, gán Employee, xác nhận đóng (SCR-41 Drawer); Employee cập nhật tiến độ (SCR-61 — **page mới**); lifecycle **Open → Assigned → In Progress → Resolved → Closed** với state machine enforcement; emit notification events cho FR-15. Stack: **Spring Boot 3 + JPA + multipart upload** + **React/TypeScript** — migrate existing maintenance pages từ `/api/maintenance-tickets` → `/api/v1/**`; refactor Manager detail (tách assign vs verify close, bỏ manager skip-status).

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA, Flyway; Spring MultipartFile upload; Spring ApplicationEvent (notification stub)  
**Storage**: SQL Server — `maintenance_tickets`, `attachments` (V029); read `bookings`, `rooms`, `properties`, `users`  
**Testing**: JUnit 5 + Mockito; `MaintenanceTicketStatusServiceTest` transitions; `MaintenanceTicketServiceTest` active-booking gate; `MaintenanceControllerIT` RBAC + scope  
**Target Platform**: Web application  
**Project Type**: `Frontend/` + `Backend/`  
**Performance Goals**: List p95 < 500ms; create with 5 images p95 < 3s; status update p95 < 1s  
**Constraints**: Status state machine strict; Open-only edit/delete; same-property assignee; soft-delete; image JPEG/PNG/WebP max 5×5MB; `@Transactional` on create+attachments  
**Scale/Scope**: ~14 REST endpoints; 6 user stories (US6 P2 notifications); SCR-22/23/41/61; Admin read-only global list

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Layered architecture | PASS | Controllers → Maintenance*Service → repositories |
| DTO + Bean Validation | PASS | CreateMaintenanceTicketRequest, AssignEmployeeRequest, UpdateStatusRequest |
| Security-first (RBAC, scope) | PASS | Customer own; Manager/Employee property scope; Admin read-only |
| No secrets in code | PASS | `APP_UPLOADS_DIR` from env |
| Test coverage ≥80% | PASS | State machine + scope + Open-only edit IT |
| Standard API envelope | PASS | api-spec §1 `{ success, message, data }` |
| Audit log TICKET_* | PASS | create, assign, status, close, delete |
| Active booking validation | PASS | FR-004 spec + §7 validation rules |

**Post-design re-check**: PASS — `MaintenanceTicketStatusService` single transition gate; FR-15 via `MaintenanceNotificationPublisher` event only.

## Project Structure

### Documentation (this feature)

```text
specs/013-maintenance-management/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/maintenance-api.yaml
├── spec.md
└── tasks.md              # /speckit-tasks (next)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controllers/
│   ├── CustomerMaintenanceController.java    # SCR-22/23 — /maintenance-tickets/me
│   ├── ManagerMaintenanceController.java     # SCR-41 — list/assign/close
│   ├── EmployeeMaintenanceController.java    # SCR-61 — list/status
│   └── AdminMaintenanceController.java       # Admin read-only list/detail
├── dtos/maintenance/
│   ├── MaintenanceTicketSummaryResponse.java
│   ├── MaintenanceTicketDetailResponse.java
│   ├── MaintenanceTicketPageResponse.java
│   ├── CreateMaintenanceTicketRequest.java
│   ├── UpdateMaintenanceTicketRequest.java
│   ├── AssignEmployeeRequest.java
│   ├── UpdateMaintenanceStatusRequest.java
│   └── CloseMaintenanceTicketRequest.java
├── entities/
│   ├── MaintenanceTicket.java
│   └── Attachment.java                       # shared EntityType enum
├── enums/
│   ├── MaintenanceTicketStatus.java
│   └── AttachmentEntityType.java
├── repositories/
│   ├── MaintenanceTicketRepository.java
│   └── AttachmentRepository.java
├── services/
│   ├── MaintenanceTicketService.java
│   ├── MaintenanceTicketStatusService.java   # state machine
│   ├── MaintenanceAttachmentService.java
│   ├── MaintenanceBookingValidator.java      # active booking gate
│   └── MaintenanceNotificationPublisher.java # FR-15 stub events
├── events/
│   └── MaintenanceTicketStatusChangedEvent.java
└── configs/
    └── FileUploadConfig.java                 # max 5MB, image types

Backend/src/main/resources/db/migration/
└── V029__maintenance_tickets_fr13.sql

Backend/src/test/java/com/homestay/
├── unit/MaintenanceTicketStatusServiceTest.java
├── unit/MaintenanceBookingValidatorTest.java
└── integration/MaintenanceControllerIT.java

Frontend/src/
├── api/maintenanceApi.ts                     # migrate → /api/v1/**
├── pages/customer/
│   └── MaintenancePages.tsx                  # SCR-22/23 (consolidate duplicates)
├── pages/manager/
│   ├── MaintenanceMgmtListPage.tsx           # SCR-41 + assign Drawer
│   └── MaintenanceMgmtDetailPage.tsx         # refactor: assign + close only
└── pages/employee/
    └── MaintenanceWorkspacePage.tsx          # SCR-61 NEW
```

**Structure Decision**: FR-13 **owns** `maintenance_tickets` + MAINTENANCE attachments. **Consumes** FR-04 bookings (active check), FR-06 property scope, FR-08 rooms. **Emits** status events to FR-15 stub. Frontend maintenance UI **exists** for Customer/Manager — migrate API + fix Manager role violations + add Employee workspace.

## Implementation Phases

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | Flyway V029 `maintenance_tickets` + `attachments` | data-model.md |
| **B** | Entities, enums, repos, DTOs, FileUploadConfig | Foundational |
| **C** | MaintenanceBookingValidator + MaintenanceTicketService create/list/detail | US-1 |
| **D** | MaintenanceAttachmentService multipart create/update | US-1, US-2 |
| **E** | Customer update/delete Open-only + CustomerMaintenanceController | US-2 |
| **F** | ManagerMaintenanceController list/assign + property scope | US-3 |
| **G** | EmployeeMaintenanceController list/status transitions | US-4 |
| **H** | Manager close (Resolved→Closed) + resolution note | US-5 |
| **I** | MaintenanceNotificationPublisher + ActivityLog | US-6, FR-015 |
| **J** | AdminMaintenanceController read-only | US-6, FR-017 |
| **K** | Frontend migration + Employee SCR-61 + Manager refactor | All US |
| **L** | Tests + quickstart | SC-001–SC-009 |

## Frontend Gap Analysis

| Item | Hiện tại | Target |
|------|----------|--------|
| Customer list | `GET /api/maintenance-tickets` | `GET /api/v1/maintenance-tickets/me` |
| Create | `POST /api/maintenance-tickets` | `POST /api/v1/maintenance-tickets` multipart |
| Manager list | `GET /api/maintenance-tickets/all` | `GET /api/v1/manager/maintenance-tickets?propertyId=` |
| Manager status | `PUT .../status` any role | Manager: `PATCH .../assign`, `PATCH .../close` only |
| Employee | **missing page** | `MaintenanceWorkspacePage.tsx` SCR-61 |
| Status values | OPEN, IN_PROGRESS (skip Assigned) | OPEN, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED |
| Manager detail | Manager sets IN_PROGRESS/RESOLVED | Employee only; Manager assign + close |
| Duplicate pages | MaintenanceListPage + MaintenancePages | Consolidate to MaintenancePages.tsx |

## Risks

| Risk | Mitigation |
|------|------------|
| FR-04 bookings not implemented | Blocker — seed Confirmed booking in Flyway test data |
| FR-06 employee list missing | Manager assign dropdown uses `GET /manager/employees?propertyId=` stub or seed |
| FR-15 not ready | `MaintenanceNotificationPublisher` logs + Outbox stub; workflow works without push |
| Manager UI bypasses Employee steps | Refactor MaintenanceMgmtDetailPage per entity-ui-mapping §2.3 |
| Attachment table shared with Damage | `entity_type` discriminator; FR-13 owns MAINTENANCE rows only |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/maintenance-api.yaml](./contracts/maintenance-api.yaml)
- [quickstart.md](./quickstart.md)

## Next Step

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md`.
