# Tasks: FR-15 Notification Engine (Event-Driven)

**Input**: Design documents from `specs/015-notification-engine/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/notification-api.yaml, quickstart.md

**Phụ thuộc**: FR-01 (JWT auth REST + WebSocket); FR-04 (`outbox_events` V023, booking lifecycle); FR-10 (contract PDF worker — separate outbox types); FR-12 (payment events); FR-13 (`MAINTENANCE_STATUS_CHANGED` stub); FR-05 (calendar WebSocket enrichment). **Ranh giới**: FR-15 owns notification delivery + inbox UI; domain FRs emit outbox only; email/SMS out of scope v1.

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
- **Contract**: `specs/015-notification-engine/contracts/notification-api.yaml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Notifications schema + verify shared outbox — after FR-04 V023

- [X] T001 Verify FR-04 `outbox_events` table applied per `specs/004-booking-inventory/quickstart.md` (blocker before V031)
- [X] T002 Create Flyway `Backend/src/main/resources/db/migration/V031__notifications_fr15.sql` — `notifications` table, indexes, dedupe unique index per `data-model.md`
- [X] T003 [P] Confirm Vite proxy `/api/v1` and `/ws` → backend in `Frontend/vite.config.ts`
- [X] T004 [P] Add `spring-boot-starter-websocket` dependency in `Backend/pom.xml` if missing
- [X] T005 [P] Add optional dev seed (1 `NOTIFICATION_DISPATCH` PENDING outbox + sample notification) in `V031__notifications_fr15.sql`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Notification entity, DTOs, service/worker skeletons, security — MUST complete before user story phases

**⚠️ CRITICAL**: No user story work until this phase complete

- [X] T006 [P] Verify or reuse `OutboxEvent.java` and `OutboxEventRepository.java` from FR-04 in `Backend/src/main/java/com/homestay/entities/OutboxEvent.java` and `Backend/src/main/java/com/homestay/repositories/OutboxEventRepository.java`
- [X] T007 [P] Create `NotificationType.java` in `Backend/src/main/java/com/homestay/enums/NotificationType.java` per `data-model.md`
- [X] T008 [P] Create `RelatedEntityType.java` in `Backend/src/main/java/com/homestay/enums/RelatedEntityType.java`
- [X] T009 [P] Create `Notification.java` entity in `Backend/src/main/java/com/homestay/entities/Notification.java` per `data-model.md`
- [X] T010 [P] Create `NotificationRepository.java` in `Backend/src/main/java/com/homestay/repositories/NotificationRepository.java` — `findByUserId`, `countByUserIdAndIsReadFalse`, `markAllReadByUserId`, dedupe lookup
- [X] T011 [P] Create notification DTOs in `Backend/src/main/java/com/homestay/dtos/notification/` — `NotificationSummaryResponse`, `NotificationDetailResponse`, `NotificationPageResponse`, `UnreadCountResponse`, `MarkAllReadResult` per `contracts/notification-api.yaml`
- [X] T012 Create `NotificationService.java` skeleton in `Backend/src/main/java/com/homestay/services/NotificationService.java`
- [X] T013 Create `NotificationOutboxPublisher.java` skeleton in `Backend/src/main/java/com/homestay/services/NotificationOutboxPublisher.java`
- [X] T014 Create `NotificationDispatchHandler.java` skeleton in `Backend/src/main/java/com/homestay/services/NotificationDispatchHandler.java`
- [X] T015 Create `NotificationOutboxWorker.java` skeleton in `Backend/src/main/java/com/homestay/services/NotificationOutboxWorker.java`
- [X] T016 Create `NotificationWebSocketPublisher.java` skeleton in `Backend/src/main/java/com/homestay/services/NotificationWebSocketPublisher.java`
- [X] T017 Register authenticated routes `GET/PATCH /api/v1/notifications/**` in `Backend/src/main/java/com/homestay/configs/SecurityConfig.java`
- [X] T018 Enable `@EnableScheduling` for outbox worker in `Backend/src/main/java/com/homestay/HomestayApplication.java` (or existing config class)

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Hệ thống gửi thông báo từ sự kiện nghiệp vụ qua Outbox (Priority: P1) 🎯 MVP

**Goal**: Outbox worker creates in-app Notification rows from business events; idempotent; retry/FAILED

**Independent Test**: Insert `NOTIFICATION_DISPATCH` PENDING outbox → worker runs → notification row for target userId; replay same dedupeKey → no duplicate

### Implementation

- [X] T019 [US1] Implement `enqueue(NotificationDispatchCommand)` transactional insert into `outbox_events` in `NotificationOutboxPublisher.java`
- [X] T020 [US1] Implement `handleNotificationDispatch(payload)` — parse JSON, create Notification row in `NotificationDispatchHandler.java`
- [X] T021 [US1] Implement idempotent create via `dedupe_key` + catch unique violation in `NotificationDispatchHandler.java`
- [X] T022 [US1] Map `BOOKING_CONFIRMED` outbox payload → `BOOKING_CONFIRMED` notification in `NotificationDispatchHandler.java`
- [X] T023 [US1] Map `PAYMENT_PENDING_VERIFICATION` → Manager recipient notification in `NotificationDispatchHandler.java`
- [X] T024 [US1] Map `PAYMENT_CONFIRMED` → Customer notification in `NotificationDispatchHandler.java`
- [X] T025 [US1] Map `MAINTENANCE_STATUS_CHANGED` (FR-13 payload) → `MAINTENANCE_UPDATED` in `NotificationDispatchHandler.java`
- [X] T026 [US1] Map `CONTRACT_GENERATED` in-app leg → `CONTRACT_GENERATED` notification in `NotificationDispatchHandler.java`
- [X] T027 [US1] Skip inactive users — mark outbox PROCESSED with log in `NotificationDispatchHandler.java`
- [X] T028 [US1] Implement `@Scheduled(fixedDelay = 15000)` batch poll with `FOR UPDATE SKIP LOCKED` in `NotificationOutboxWorker.java`
- [X] T029 [US1] Implement retry `retry_count` increment and FAILED after max 3 in `NotificationOutboxWorker.java`
- [X] T030 [US1] Filter worker to notification `event_type` list only (exclude FR-10 contract types) in `NotificationOutboxWorker.java`
- [X] T031 [US1] Log `NOTIFICATION_CREATED`, `OUTBOX_PROCESSED`, `OUTBOX_FAILED` to ActivityLog in `NotificationDispatchHandler.java` and `NotificationOutboxWorker.java`
- [X] T032 [US1] Wire FR-04 `DepositConfirmationService` or booking confirm path to enqueue `BOOKING_CONFIRMED` in `Backend/src/main/java/com/homestay/services/DepositConfirmationService.java` (stub OK if FR-04 not merged)
- [X] T033 [US1] Wire FR-13 `MaintenanceNotificationPublisher` to insert `MAINTENANCE_STATUS_CHANGED` outbox in `Backend/src/main/java/com/homestay/services/MaintenanceNotificationPublisher.java`

**Checkpoint**: US1 MVP — outbox → notification row testable via SQL seed + worker poll

---

## Phase 4: User Story 2 — Danh sách thông báo và số chưa đọc (Priority: P1)

**Goal**: Paginated inbox list + unread count + filter; all authenticated roles; SCR-13

**Independent Test**: User A GET /notifications → only A's rows; unread count matches; unreadOnly filter works; user B's notification → 403/404

### Implementation

- [X] T034 [US2] Implement `listForUser(userId, pageable, unreadOnly)` — newest first in `NotificationService.java`
- [X] T035 [US2] Implement `getUnreadCount(userId)` in `NotificationService.java`
- [X] T036 [US2] Create `NotificationController.java` with `GET /api/v1/notifications` in `Backend/src/main/java/com/homestay/controllers/NotificationController.java`
- [X] T037 [US2] Add `GET /api/v1/notifications/unread-count` in `NotificationController.java`
- [X] T038 [US2] Enforce current-user scope on all list/count queries in `NotificationService.java`
- [X] T039 [P] [US2] Migrate `getNotifications` and `getUnreadCount` to `/api/v1/notifications` paths in `Frontend/src/api/notificationApi.ts`
- [X] T040 [US2] Wire live list + ALL/UNREAD filter + pagination on `Frontend/src/pages/customer/NotificationPages.tsx` `NotificationCenterPage`
- [X] T041 [US2] Wire unread badge fetch on mount in `Frontend/src/layouts/CustomerLayout.tsx`
- [X] T042 [US2] Wire unread badge in `Frontend/src/layouts/ManagerLayout.tsx`
- [X] T043 [P] [US2] Add notification routes `/manager/notifications`, `/employee/notifications`, `/admin/notifications` reusing `NotificationPages` in `Frontend/src/App.tsx`

**Checkpoint**: US2 testable — SCR-13 list + badge for Customer/Manager

---

## Phase 5: User Story 3 — Chi tiết và đánh dấu đã đọc (Priority: P1)

**Goal**: Detail view auto mark read; PATCH single read; mark all read; SCR-14

**Independent Test**: Open detail → isRead true; PATCH read → count decreases; mark-all → count 0; cross-user → 403/404

### Implementation

- [X] T044 [US3] Implement `getDetailForUser(notificationId, userId, markRead)` in `NotificationService.java`
- [X] T045 [US3] Implement `markAsRead(notificationId, userId)` in `NotificationService.java`
- [X] T046 [US3] Implement `markAllAsRead(userId)` returning updated count in `NotificationService.java`
- [X] T047 [US3] Add `GET /api/v1/notifications/{id}?markRead=true` in `NotificationController.java`
- [X] T048 [US3] Add `PATCH /api/v1/notifications/{id}/read` in `NotificationController.java`
- [X] T049 [US3] Add `PATCH /api/v1/notifications/mark-all-read` in `NotificationController.java`
- [X] T050 [US3] Log `NOTIFICATION_MARKED_READ` and `NOTIFICATION_MARKED_ALL_READ` to ActivityLog in `NotificationService.java`
- [X] T051 [P] [US3] Add `getNotificationDetail` and `markAllRead` v1 paths; add `markAsRead(id)` in `Frontend/src/api/notificationApi.ts`
- [X] T052 [US3] Wire detail fetch + auto mark read on `Frontend/src/pages/customer/NotificationPages.tsx` `NotificationDetailPage`
- [X] T053 [US3] Wire Mark all as read button to live API on `NotificationPages.tsx` `NotificationCenterPage`
- [X] T054 [US3] Dispatch `unreadCountChanged` window event after mark-read operations in `NotificationPages.tsx`

**Checkpoint**: US3 testable — detail + mark read flows

---

## Phase 6: User Story 4 — Push thông báo real-time qua WebSocket (Priority: P1)

**Goal**: STOMP push on new notification; calendar refresh topic stub; online badge update

**Independent Test**: Client subscribed with JWT → worker creates notification → WS message within 5s; disconnect → REST inbox still complete

### Implementation

- [X] T055 [P] [US4] Add `@stomp/stompjs` and `sockjs-client` dependencies in `Frontend/package.json` if missing
- [X] T056 [US4] Create `WebSocketConfig.java` — endpoint `/ws`, broker `/topic`, app prefix in `Backend/src/main/java/com/homestay/configs/WebSocketConfig.java`
- [X] T057 [US4] Create `WebSocketAuthChannelInterceptor.java` — validate JWT on CONNECT in `Backend/src/main/java/com/homestay/configs/WebSocketAuthChannelInterceptor.java`
- [X] T058 [US4] Implement `publishToUser(userId, NotificationPushMessage)` on `/topic/users/{userId}/notifications` in `NotificationWebSocketPublisher.java`
- [X] T059 [US4] Call `NotificationWebSocketPublisher` after successful notification create in `NotificationDispatchHandler.java`
- [X] T060 [US4] Implement `publishCalendarRefresh(propertyId, payload)` on `/topic/properties/{propertyId}/calendar` stub in `NotificationWebSocketPublisher.java`
- [X] T061 [US4] Register WebSocket endpoint security in `SecurityConfig.java`
- [X] T062 [US4] Create `useNotificationWebSocket.ts` — connect, subscribe, dispatch `unreadCountChanged` in `Frontend/src/hooks/useNotificationWebSocket.ts`
- [X] T063 [US4] Integrate `useNotificationWebSocket` in `Frontend/src/layouts/CustomerLayout.tsx`
- [X] T064 [US4] Integrate `useNotificationWebSocket` in `Frontend/src/layouts/ManagerLayout.tsx`
- [X] T065 [US4] Add reconnect + 60s REST fallback refetch on disconnect in `Frontend/src/hooks/useNotificationWebSocket.ts`

**Checkpoint**: US4 testable — real-time push + badge refresh

---

## Phase 7: User Story 5 — Điều hướng nhanh từ thông báo (Priority: P2)

**Goal**: Context action buttons per notification type; role-aware paths; SCR-14

**Independent Test**: BOOKING_CONFIRMED → View Booking navigates to correct role path; SYSTEM → no action button

### Implementation

- [X] T066 [P] [US5] Create `getNotificationActionPath(type, relatedEntityType, relatedEntityId, role)` in `Frontend/src/utils/notificationDeepLink.ts`
- [X] T067 [US5] Map BOOKING_CONFIRMED, CONTRACT_GENERATED, PAYMENT_CONFIRMED, PAYMENT_PENDING_VERIFICATION, MAINTENANCE_UPDATED paths per `research.md` §11 in `notificationDeepLink.ts`
- [X] T068 [US5] Detect current user role from auth store/context for path selection in `notificationDeepLink.ts`
- [X] T069 [US5] Add context action button on `NotificationDetailPage` in `Frontend/src/pages/customer/NotificationPages.tsx`
- [X] T070 [US5] Hide action button when `relatedEntityId` null or type SYSTEM on `NotificationPages.tsx`

**Checkpoint**: US5 testable — one-click navigation from detail

---

## Phase 8: User Story 6 — Admin giám sát Outbox thất bại (Priority: P2)

**Goal**: Admin list FAILED notification outbox events; manual retry; SCR-56 tab

**Independent Test**: Force worker failure → FAILED row visible to Admin → retry → PROCESSED on success

### Implementation

- [X] T071 [US6] Implement `listOutboxEvents(status, pageable)` filtered to notification worker event types in `NotificationOutboxWorker.java` or dedicated `OutboxAdminService.java`
- [X] T072 [US6] Implement `retryFailedEvent(outboxId)` — reset FAILED → PENDING in `OutboxAdminService.java`
- [X] T073 [US6] Create `AdminOutboxController.java` with `GET /api/v1/admin/outbox-events` and `POST /api/v1/admin/outbox-events/{id}/retry` in `Backend/src/main/java/com/homestay/controllers/AdminOutboxController.java`
- [X] T074 [US6] Restrict admin endpoints to ROLE_ADMIN in `SecurityConfig.java`
- [X] T075 [US6] Log `OUTBOX_RETRY_REQUESTED` to ActivityLog in `OutboxAdminService.java`
- [X] T076 [US6] Add Outbox Failures tab wired to admin API in `Frontend/src/pages/admin/AdminPages.tsx`
- [X] T077 [US6] Add retry button + status display on Admin outbox tab in `Frontend/src/pages/admin/AdminPages.tsx`

**Checkpoint**: US6 testable — admin monitor + retry

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Producer stubs, tests, quickstart validation, cleanup

- [X] T078 [P] Wire FR-12 payment confirm/pending paths to enqueue payment outbox events in `Backend/src/main/java/com/homestay/services/PaymentService.java` (stub OK if FR-12 not merged)
- [X] T079 [P] Wire FR-10 contract worker to enqueue `NOTIFICATION_DISPATCH` CONTRACT_GENERATED after PDF ready in `Backend/src/main/java/com/homestay/services/ContractOutboxWorker.java` (stub OK)
- [X] T080 [P] Unit test idempotent dispatch + inactive user skip in `Backend/src/test/java/com/homestay/unit/NotificationDispatchHandlerTest.java`
- [X] T081 [P] Unit test retry → FAILED after max attempts in `Backend/src/test/java/com/homestay/unit/NotificationOutboxWorkerTest.java`
- [X] T082 Integration test list scope + mark read + ownership in `Backend/src/test/java/com/homestay/integration/NotificationControllerIT.java`
- [X] T083 [P] Integration test WS push on notification create in `Backend/src/test/java/com/homestay/integration/NotificationWebSocketIT.java`
- [X] T084 Run curl smoke tests in `specs/015-notification-engine/quickstart.md` and fix gaps
- [X] T085 [P] Remove `deleteNotification` and `seedNotifications` dev mocks from `Frontend/src/api/notificationApi.ts`
- [X] T086 [P] Verify all notification routes in `Frontend/src/App.tsx` (customer/manager/employee/admin)
- [X] T087 [P] Wire dashboard notification preview widget on `Frontend/src/pages/customer/CustomerDashboardPage.tsx` to live unread API

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: After FR-04 `outbox_events` V023 (blocker)
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **US1 (P1)**: After Foundational — **MVP**; creates notifications consumed by US2–US4
- **US2 (P1)**: After US1 (needs notification rows; can seed manually for parallel dev)
- **US3 (P1)**: After US2 (shared NotificationController/Service)
- **US4 (P1)**: After US1 (push on create); frontend hook parallel after T058
- **US5 (P2)**: After US3 (detail page exists)
- **US6 (P2)**: After US1 (FAILED outbox from worker); independent of US2–US5
- **Polish (Phase 9)**: After desired user stories complete

### User Story Dependencies

```text
Foundational → US1 → US2 → US3 → US5
                    → US4 (after US1 create path)
                    → US6 (after US1 worker)
```

### Parallel Opportunities

**Phase 1**: T003, T004, T005 all [P]  
**Phase 2** (after T002): T006–T011 all [P]  
**US2**: T039 parallel with T034–T038; T043 parallel after T040  
**US3**: T051 parallel with T044–T050  
**US4**: T055 parallel with T056–T058; T062–T064 after hook created  
**US5**: T066 parallel with T067–T068  
**US6**: T076 parallel after T073  
**Polish**: T078, T079, T080, T081, T083, T085, T086, T087 all [P]

### Parallel Example: User Story 1

```bash
T019–T021 NotificationOutboxPublisher + DispatchHandler core
T028–T030 NotificationOutboxWorker poll/retry
T032–T033 Producer wiring (FR-04, FR-13)
```

### Parallel Example: User Story 4

```bash
T056–T058 WebSocket backend config + publisher
T055 stompjs frontend deps
T062–T065 useNotificationWebSocket + layouts
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T005)
2. Complete Phase 2: Foundational (T006–T018)
3. Complete Phase 3: User Story 1 (T019–T033)
4. **STOP and VALIDATE**: SQL seed outbox → wait 15s → notification row exists per `quickstart.md`
5. Demo worker idempotency (replay → no duplicate)

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 → Outbox → notification delivery (MVP)
3. US2 → Inbox list + unread badge (SCR-13)
4. US3 → Detail + mark read (SCR-14)
5. US4 → WebSocket real-time push
6. US5 → Deep link action buttons
7. US6 → Admin outbox monitor
8. Polish → Producer stubs + tests + quickstart

### Parallel Team Strategy

1. Team completes Setup + Foundational together
2. After Foundational:
   - Dev A: US1 + US6 (worker + admin)
   - Dev B: US2 + US3 (REST inbox)
   - Dev C: US4 + US5 (WebSocket + deep links)
3. Polish when core paths work

---

## Notes

- FR-10 `ContractOutboxWorker` handles `CONTRACT_GENERATE_REQUESTED` only — never in notification worker filter
- Delete notification API **omitted** v1 per research.md §15
- `message` vs `content` in api-spec SCR-13 — DTO uses `content`; alias if needed for backward compat
- Manager `/manager/notifications` link exists in layout but route missing — fixed in T043
- Employee inbox reuses same `NotificationPages` under Employee layout wrapper
- Commit after each task or logical group; stop at any checkpoint to validate story independently
