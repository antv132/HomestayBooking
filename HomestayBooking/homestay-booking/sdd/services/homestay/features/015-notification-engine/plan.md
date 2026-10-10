# Implementation Plan: FR-15 Notification Engine (Event-Driven)

**Branch**: `017-notification-engine` | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/015-notification-engine/spec.md`

**Nguồn docs**: `docs/Specification_v2.md` (FR-15, §5 Notification/OutboxEvent), `docs/api-spec-by-screen.md` (SCR-13/14), `docs/screen.md`, `docs/screendesign.md`, `docs/entity-ui-mapping.md` §1.14, `docs/figma-generation-prompt.md` (Notification types), `AGENTS.md` (Outbox pattern), frontend `notificationApi.ts`, `NotificationPages.tsx`, `CustomerLayout.tsx`, `ManagerLayout.tsx`

**Phụ thuộc**: FR-01 (auth JWT — all roles inbox); FR-04 (`outbox_events` V023, booking lifecycle); FR-10 (contract PDF/email worker — separate outbox types); FR-12 (payment events); FR-13 (`MAINTENANCE_STATUS_CHANGED` outbox stub); FR-05 (availability calendar WebSocket enrichment). **Ranh giới**: FR-15 owns `notifications` table, notification Outbox worker, WebSocket push, inbox REST + UI (SCR-13/14), Admin outbox monitoring; domain FRs **emit** outbox rows via `NotificationOutboxPublisher` — FR-15 does **not** own booking/payment/maintenance business logic; email/SMS out of scope v1.

## Summary

Triển khai **FR-15 Notification Engine**: bảng `notifications` (V031); worker `NotificationOutboxWorker` poll `outbox_events` loại `NOTIFICATION_DISPATCH` + mapped domain events (`MAINTENANCE_STATUS_CHANGED`, `BOOKING_CONFIRMED`, `PAYMENT_*`, `CONTRACT_GENERATED` in-app leg); idempotent create + `NotificationWebSocketPublisher` STOMP push; REST inbox (`GET` list/detail, unread count, mark read/all) cho **mọi role** authenticated; Admin tab SCR-56 monitor/retry FAILED outbox; frontend migrate `notificationApi.ts` → `/api/v1/**`, add `useNotificationWebSocket`, deep-link actions per type, shared routes cho Manager/Employee/Admin.

## Technical Context

**Language/Version**: Java 17+, TypeScript 5.x, React 18  
**Primary Dependencies**: Spring Boot 3.x, Spring Security, Spring Data JPA, Spring WebSocket/STOMP, Flyway, Bean Validation  
**Storage**: SQL Server — `notifications` (V031); read/write `outbox_events` (FR-04 V023); read `users` for active check  
**Testing**: JUnit 5 + Mockito; `NotificationOutboxWorkerTest` idempotency; `NotificationControllerIT` ownership + mark-read; `NotificationWebSocketIT` push on create  
**Target Platform**: Web application  
**Project Type**: `Frontend/` + `Backend/`  
**Performance Goals**: Outbox→notification p95 < 30s; WebSocket push p95 < 5s; inbox list p95 < 500ms  
**Constraints**: User-scoped inbox only; idempotent notification create; max outbox retry 3; worker poll 15s; no email/SMS v1  
**Scale/Scope**: ~8 REST endpoints + 1 WebSocket topic; 6 user stories; SCR-13/14 all roles + Admin outbox tab

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| Layered architecture | PASS | Controllers → NotificationService / Worker → repositories |
| DTO + Bean Validation | PASS | Page params; ownership on `{id}` |
| Security-first (RBAC, scope) | PASS | All AUTH inbox own-only; Admin outbox monitor |
| No secrets in code | PASS | JWT for WS auth via existing FR-01 |
| Test coverage ≥80% | PASS | Idempotency + ownership + mark-read IT |
| Standard API envelope | PASS | api-spec §1 |
| Audit log | PASS | NOTIFICATION_CREATED, OUTBOX_PROCESSED, OUTBOX_FAILED |
| Outbox transactional | PASS | Producers write outbox in same TX (FR-04 pattern) |

**Post-design re-check**: PASS — separate workers per event family; `NotificationOutboxPublisher` shared helper for producers; FR-10 contract PDF worker unchanged.

## Project Structure

### Documentation (this feature)

```text
specs/015-notification-engine/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/notification-api.yaml
├── spec.md
└── tasks.md              # /speckit-tasks (next)
```

### Source Code (repository root)

```text
Backend/src/main/java/com/homestay/
├── controllers/
│   ├── NotificationController.java           # SCR-13/14 — /notifications/**
│   └── AdminOutboxController.java            # SCR-56 — /admin/outbox-events/**
├── dtos/notification/
│   ├── NotificationSummaryResponse.java
│   ├── NotificationDetailResponse.java
│   ├── NotificationPageResponse.java
│   └── UnreadCountResponse.java
├── entities/
│   └── Notification.java
├── enums/
│   ├── NotificationType.java
│   └── RelatedEntityType.java
├── repositories/
│   └── NotificationRepository.java
├── services/
│   ├── NotificationService.java              # list, detail, mark read, unread count
│   ├── NotificationOutboxPublisher.java        # helper for FR-04/12/13/10 producers
│   ├── NotificationOutboxWorker.java         # @Scheduled poll + retry
│   ├── NotificationDispatchHandler.java        # payload → Notification row
│   ├── NotificationWebSocketPublisher.java     # STOMP push after create
│   └── NotificationDeepLinkResolver.java       # relatedEntityType → path hint (optional)
├── configs/
│   ├── WebSocketConfig.java
│   └── WebSocketAuthChannelInterceptor.java
└── (reuse from FR-04)
    ├── entities/OutboxEvent.java
    └── repositories/OutboxEventRepository.java

Backend/src/main/resources/db/migration/
└── V031__notifications_fr15.sql

Backend/src/test/java/com/homestay/
├── unit/NotificationDispatchHandlerTest.java
├── unit/NotificationOutboxWorkerTest.java
└── integration/NotificationControllerIT.java

Frontend/src/
├── api/notificationApi.ts                    # migrate → /api/v1/notifications/**
├── hooks/useNotificationWebSocket.ts         # STOMP subscribe + badge refresh
├── pages/customer/
│   └── NotificationPages.tsx                 # SCR-13/14 — WS + deep links
├── layouts/
│   ├── CustomerLayout.tsx                    # live unread badge
│   └── ManagerLayout.tsx                     # wire badge + route
└── App.tsx                                   # /manager/notifications, /employee/notifications, /admin/notifications
```

**Structure Decision**: FR-15 **owns** `notifications` table and notification dispatch worker. **Consumes** shared `outbox_events` (FR-04). **Feeds** all role layouts via REST + WebSocket. Frontend notification UI **exists** — migrate API paths, add WS hook, extend routes beyond Customer-only.

## Implementation Phases

| Phase | Scope | Traceability |
|-------|-------|--------------|
| **A** | Flyway V031 `notifications` + dedupe index | data-model.md |
| **B** | Notification entity, enums, repo, DTOs | Foundational |
| **C** | `NotificationOutboxPublisher` + payload schema | US-1 producers |
| **D** | `NotificationDispatchHandler` + idempotency | US-1 |
| **E** | `NotificationOutboxWorker` poll/retry/FAILED | US-1, US-6 |
| **F** | `NotificationService` + `NotificationController` REST | US-2, US-3 |
| **G** | WebSocket STOMP config + `NotificationWebSocketPublisher` | US-4 |
| **H** | `NotificationDeepLinkResolver` + frontend action buttons | US-5 |
| **I** | `AdminOutboxController` FAILED list + retry | US-6 |
| **J** | Frontend migration + `useNotificationWebSocket` + multi-role routes | US-2–US-5 |
| **K** | Wire producer stubs (FR-13 maintenance, FR-12 payment, FR-04 booking) | US-1 integration |
| **L** | Tests + quickstart | SC-001–SC-008 |

## Frontend Gap Analysis

| Item | Hiện tại | Target |
|------|----------|--------|
| API base | `/api/notifications` | `/api/v1/notifications` |
| Mark single read | local state only on click | `PATCH /notifications/{id}/read` + detail auto-read |
| WebSocket | none (poll via layout) | `useNotificationWebSocket` STOMP |
| Manager route | link `/manager/notifications` **missing** in App.tsx | Add route reusing `NotificationPages` |
| Employee/Admin inbox | not routed | Shared pages under role layouts |
| Deep links | partial in detail page | Per-type navigation map |
| Delete notification | `deleteNotification` in API | **P2 optional** — omit v1 unless needed |
| test-seed endpoint | dev mock | Remove after real backend |
| Calendar refresh | N/A | WS `CALENDAR_REFRESH` payload (FR-05) without per-cell Notification |

## Producer Integration (emit only — FR-15 delivers)

| Source FR | Trigger | Outbox / Publisher call | Recipient |
|-----------|---------|-------------------------|-----------|
| FR-04 | Booking CONFIRMED | `BOOKING_CONFIRMED` or `NOTIFICATION_DISPATCH` | Customer |
| FR-10 | Contract PDF ready | `NOTIFICATION_DISPATCH` type CONTRACT_GENERATED | Customer |
| FR-12 | Payment Pending (bank) | `PAYMENT_PENDING_VERIFICATION` | Manager (property) |
| FR-12 | Payment Confirmed | `PAYMENT_CONFIRMED` | Customer |
| FR-13 | Ticket status change | `MAINTENANCE_STATUS_CHANGED` | Customer |
| FR-05 | Material calendar change | WS `CALENDAR_REFRESH` only (no Notification row) | Manager/Employee scope |
| Admin | System broadcast P2 | `NOTIFICATION_DISPATCH` type SYSTEM | Target users |

## Risks

| Risk | Mitigation |
|------|------------|
| FR-04 outbox table not migrated | Blocker — verify V023 before V031 |
| Multiple workers race on same outbox row | `SELECT … FOR UPDATE SKIP LOCKED` in worker |
| WebSocket JWT expiry | Reject CONNECT; client refetch REST + reconnect |
| Duplicate notifications on replay | UNIQUE dedupe index + handler check |
| FR-10/FR-15 both listen outbox | Separate `event_type` filters per worker |
| High-frequency calendar updates | Broadcast WS only; no Notification persistence per cell |
| Manager notifications route 404 | Add App.tsx routes in phase J |

## Generated Artifacts

- [research.md](./research.md)
- [data-model.md](./data-model.md)
- [contracts/notification-api.yaml](./contracts/notification-api.yaml)
- [quickstart.md](./quickstart.md)

## Next Step

Run **`/speckit-tasks`** to generate dependency-ordered `tasks.md`.
