# Research: FR-15 Notification Engine (Event-Driven)

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Docs**: `notificationApi.ts`, `NotificationPages.tsx`, FR-04 `outbox_events`, FR-13 §9

## 1. Table Ownership

**Decision**: FR-15 **owns** `notifications` table (V031). **Shares** `outbox_events` (FR-04 V023) — FR-15 owns `NotificationOutboxWorker` for notification-related `event_type` values only.

**Rationale**: Spec FR-001/FR-002; Specification §5 separates Notification entity from OutboxEvent; FR-10 `ContractOutboxWorker` and FR-15 worker coexist with disjoint `event_type` filters.

**Alternatives considered**: Dedicated `notification_outbox` table — rejected (duplicate infra; spec mandates shared OutboxEvent).

## 2. Outbox Event Types (Worker Consumption)

**Decision**: `NotificationOutboxWorker` processes:

| event_type | Source | Maps to NotificationType |
|------------|--------|--------------------------|
| `NOTIFICATION_DISPATCH` | Any producer via `NotificationOutboxPublisher` | Payload `type` field |
| `BOOKING_CONFIRMED` | FR-04 deposit confirm | BOOKING_CONFIRMED |
| `CONTRACT_GENERATED` | FR-10 after PDF ready (in-app leg) | CONTRACT_GENERATED |
| `PAYMENT_PENDING_VERIFICATION` | FR-12 bank transfer upload | PAYMENT_PENDING_VERIFICATION |
| `PAYMENT_CONFIRMED` | FR-12 VNPay/bank approve | PAYMENT_CONFIRMED |
| `MAINTENANCE_STATUS_CHANGED` | FR-13 status transition | MAINTENANCE_UPDATED |
| `SYSTEM_BROADCAST` | Admin P2 | SYSTEM |

`CONTRACT_GENERATE_REQUESTED`, `CONTRACT_EMAIL_SEND` remain **FR-10 only** — never handled by notification worker.

**Rationale**: FR-13 already stubs `MAINTENANCE_STATUS_CHANGED`; generic `NOTIFICATION_DISPATCH` keeps producer API simple for ad-hoc notifications.

**Alternatives considered**: Single generic type only — rejected (domain events already planned in upstream FR specs).

## 3. Producer Helper API

**Decision**: `NotificationOutboxPublisher.enqueue(NotificationDispatchCommand)` writes `outbox_events` row in **caller's `@Transactional`** method:

```java
record NotificationDispatchCommand(
  UUID userId,
  NotificationType type,
  String title,
  String content,
  RelatedEntityType relatedEntityType,
  UUID relatedEntityId,
  String dedupeKey  // optional; defaults to correlationId
) {}
```

Domain services (FR-04/12/13) may call publisher **or** insert typed domain event — worker normalizes both paths in `NotificationDispatchHandler`.

**Rationale**: Spec FR-016; transactional outbox per Agents.md.

**Alternatives considered**: Spring ApplicationEvent only — rejected (not durable across crash).

## 4. Idempotency

**Decision**: DB partial unique index:

```sql
UNIQUE (user_id, type, related_entity_id, dedupe_key)
WHERE related_entity_id IS NOT NULL AND dedupe_key IS NOT NULL
```

Handler catches duplicate → mark outbox PROCESSED without error. `dedupeKey` in payload: `{eventType}:{relatedEntityId}:{userId}` or explicit correlationId from producer.

**Rationale**: Spec FR-012, SC-005; prevents duplicate inbox on outbox replay.

**Alternatives considered**: Separate `notification_dedup` table — rejected (YAGNI).

## 5. Worker Scheduling & Retry

**Decision**: `@Scheduled(fixedDelay = 15000)` on `NotificationOutboxWorker.processBatch()`:

- Fetch `PENDING` where `event_type IN (...)` ORDER BY `created_at` LIMIT 50
- `SELECT … FOR UPDATE SKIP LOCKED` per row
- On success → PROCESSED + `processed_at`
- On failure → increment `retry_count`; if `>= 3` → FAILED + ActivityLog `OUTBOX_FAILED`
- Backoff: immediate retry next poll (15s interval acts as backoff)

**Rationale**: Spec assumptions; aligns with FR-10 contract worker SLA.

**Alternatives considered**: Kafka/RabbitMQ — rejected v1 (SQL Server outbox sufficient per project docs).

## 6. WebSocket Transport

**Decision**: Spring **STOMP** over WebSocket endpoint `/ws` with broker prefix `/topic`:

- Subscribe: `/topic/users/{userId}/notifications`
- Auth: JWT in `CONNECT` header `Authorization: Bearer` via `WebSocketAuthChannelInterceptor`
- Message payload `NotificationPushMessage`: `{ notificationId, type, title, summary, createdAt }`
- Calendar refresh (FR-05): separate topic `/topic/properties/{propertyId}/calendar` message `{ type: "CALENDAR_REFRESH", propertyId, roomId?, date? }` — **no** Notification row

**Rationale**: Spec FR-010; Spring Boot native STOMP; user-scoped topics prevent cross-user leak.

**Alternatives considered**: Raw WebSocket JSON — STOMP gives subscription model + Spring Security integration.

## 7. REST API Surface

**Decision**: Extend `api-spec-by-screen.md` SCR-13/14:

| Method | Path | Role | Notes |
|--------|------|------|-------|
| GET | `/api/v1/notifications` | ALL AUTH | `page`, `size`, `unreadOnly` |
| GET | `/api/v1/notifications/unread-count` | ALL AUTH | `{ count }` |
| GET | `/api/v1/notifications/{id}` | ALL AUTH | own only; auto mark read optional query `?markRead=true` |
| PATCH | `/api/v1/notifications/{id}/read` | ALL AUTH | per api-spec SCR-14 |
| PATCH | `/api/v1/notifications/mark-all-read` | ALL AUTH | `{ updated }` |

Response field `message` in api-spec → map to entity `content` in API (alias in DTO).

**Rationale**: Matches existing `notificationApi.ts`; api-spec documents list + PATCH read only.

**Alternatives considered**: POST for mark-read — PATCH matches api-spec and frontend.

## 8. Auto Mark Read on Detail

**Decision**: `GET /notifications/{id}?markRead=true` (default **true**) sets `isRead=true` when owner opens detail — matches SCR-14 figma behavior.

**Rationale**: Spec US-3 scenario 1; reduces extra PATCH round-trip.

**Alternatives considered**: PATCH-only — worse UX for detail page.

## 9. Admin Outbox Monitoring

**Decision**: `GET /api/v1/admin/outbox-events?status=FAILED&page=&size=` returns notification-worker event types only. `POST /api/v1/admin/outbox-events/{id}/retry` resets to PENDING if FAILED (manual retry US-6).

**Rationale**: entity-ui-mapping §1.14 Admin monitors OutboxEvent failures; api-spec SCR-56 lacks endpoint — add per spec US-6.

**Alternatives considered**: Full outbox admin for contract events — scope to notification `event_type` filter v1; contract failures stay FR-10 admin tab P2.

## 10. NotificationType Enum

**Decision**: `BOOKING_CONFIRMED`, `CONTRACT_GENERATED`, `PAYMENT_CONFIRMED`, `PAYMENT_PENDING_VERIFICATION`, `MAINTENANCE_UPDATED`, `AVAILABILITY_UPDATED`, `SYSTEM` — matches figma + spec FR-004 extension.

**Rationale**: Frontend `notificationApi.ts` already defines subset; add `PAYMENT_PENDING_VERIFICATION` and `AVAILABILITY_UPDATED` for completeness.

**Alternatives considered**: Free-form string — rejected (needs UI icon/routing map).

## 11. Deep Link Routing (Frontend)

**Decision**: `getNotificationAction(type, relatedEntityType, relatedEntityId, role)` returns path:

| Type | Customer path | Manager path |
|------|---------------|--------------|
| BOOKING_CONFIRMED | `/customer/bookings/{id}` | `/manager/bookings/{id}` |
| CONTRACT_GENERATED | `/customer/contracts` | `/manager/contracts` |
| PAYMENT_CONFIRMED | `/customer/payments` | `/manager/payments` |
| PAYMENT_PENDING_VERIFICATION | — | `/manager/payments/verify` |
| MAINTENANCE_UPDATED | `/customer/maintenance/{id}` | `/manager/maintenance/{id}` |
| SYSTEM | null | null |

**Rationale**: Spec US-5; role-aware navigation.

**Alternatives considered**: Backend returns `actionUrl` — frontend role routing simpler v1.

## 12. Inactive User Handling

**Decision**: Worker skips notification create if `users.status != ACTIVE`; mark outbox PROCESSED with log `SKIPPED_INACTIVE_USER`.

**Rationale**: Spec edge case assumption.

**Alternatives considered**: Create anyway — rejected (orphan notifications).

## 13. Flyway Version

**Decision**: `V031__notifications_fr15.sql` after FR-14 V030.

**Rationale**: Sequential migration numbering.

**Alternatives considered**: Combine with FR-04 outbox — rejected (feature ownership).

## 14. Frontend WebSocket Fallback

**Decision**: `useNotificationWebSocket` on connect failure or disconnect → no polling v1; layout refetches unread count on focus + after 60s `refetchInterval` until WS connected (align FR-05 calendar pattern).

**Rationale**: Spec edge case WebSocket auth expiry; pragmatic fallback without hammering server.

**Alternatives considered**: Aggressive 5s polling — rejected (load).

## 15. Delete Notification

**Decision**: **Omit** `DELETE /notifications/{id}` v1 — not in Specification_v2 FR-15 bullets; remove `deleteNotification` usage from frontend or guard behind P2 flag.

**Rationale**: YAGNI; spec marks optional.

**Alternatives considered**: Implement because frontend has method — defer P2.
