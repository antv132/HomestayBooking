# Quickstart: FR-15 Notification Engine (Event-Driven)

**Feature**: `specs/015-notification-engine` | **Plan**: [plan.md](./plan.md)

**Prerequisite**: FR-01 JWT (any role); FR-04 `outbox_events` (V023); Flyway V031 applied; WebSocket endpoint enabled.

## Prerequisites

- Java 17+, Maven 3.9+
- Node 18+
- SQL Server 2022+
- Seed: 1 active user with notifications; 1 `NOTIFICATION_DISPATCH` PENDING outbox row

## Environment

```bash
# WebSocket (optional override)
# APP_WS_ENDPOINT=/ws
```

Vite proxy: `/api/v1` → `http://localhost:8080`; `/ws` → `ws://localhost:8080/ws`

## Run

```bash
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

## Screen → Route → API

| Screen | Route | API |
|--------|-------|-----|
| SCR-13 Notification Center | `/customer/notifications` (also `/manager/notifications`, etc.) | `GET /notifications` |
| SCR-14 Notification Detail | `/customer/notifications/:id` | `GET /notifications/{id}` |
| Unread badge (layout) | — | `GET /notifications/unread-count` |
| Mark all read | button on SCR-13 | `PATCH /notifications/mark-all-read` |
| Admin outbox tab | `/admin/...` System Admin | `GET /admin/outbox-events?status=FAILED` |

## Smoke test: enqueue outbox → notification

```bash
BASE=http://localhost:8080/api/v1
USER_TOKEN="<jwt>"
USER_ID="<user-uuid>"
BOOKING_ID="<booking-uuid>"

# Insert outbox via SQL or trigger from FR-04 deposit confirm, then wait ~15s for worker

curl -s "$BASE/notifications?page=0&size=20" \
  -H "Authorization: Bearer $USER_TOKEN" | jq

curl -s "$BASE/notifications/unread-count" \
  -H "Authorization: Bearer $USER_TOKEN" | jq
# Expected: count >= 1 after worker runs
```

## Smoke test: detail + mark read

```bash
NOTIF_ID=$(curl -s "$BASE/notifications?unreadOnly=true" \
  -H "Authorization: Bearer $USER_TOKEN" | jq -r '.data.content[0].id')

curl -s "$BASE/notifications/$NOTIF_ID?markRead=true" \
  -H "Authorization: Bearer $USER_TOKEN" | jq
# Expected: isRead true

curl -s "$BASE/notifications/unread-count" \
  -H "Authorization: Bearer $USER_TOKEN" | jq
# Expected: count decreased by 1
```

## Smoke test: mark all read

```bash
curl -s -X PATCH "$BASE/notifications/mark-all-read" \
  -H "Authorization: Bearer $USER_TOKEN" | jq
# Expected: { updated: N }

curl -s "$BASE/notifications/unread-count" \
  -H "Authorization: Bearer $USER_TOKEN" | jq
# Expected: { count: 0 }
```

## Smoke test: ownership enforcement

```bash
OTHER_USER_NOTIF="<notification-uuid-owned-by-other>"
curl -s -o /dev/null -w "%{http_code}" "$BASE/notifications/$OTHER_USER_NOTIF" \
  -H "Authorization: Bearer $USER_TOKEN"
# Expected: 404 or 403
```

## Smoke test: idempotency (no duplicate)

```bash
# Replay same NOTIFICATION_DISPATCH outbox with same dedupeKey
# After worker runs twice, inbox should have exactly 1 row for that dedupe key
curl -s "$BASE/notifications?size=50" \
  -H "Authorization: Bearer $USER_TOKEN" | jq '[.data.content[] | select(.type=="BOOKING_CONFIRMED")] | length'
```

## Smoke test: Admin failed outbox

```bash
ADMIN_TOKEN="<admin-jwt>"

curl -s "$BASE/admin/outbox-events?status=FAILED" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq

FAILED_ID="<outbox-uuid>"
curl -s -X POST "$BASE/admin/outbox-events/$FAILED_ID/retry" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
# Expected: success; worker reprocesses on next poll
```

## WebSocket (STOMP)

```text
CONNECT /ws
  Header: Authorization: Bearer <jwt>

SUBSCRIBE /topic/users/{userId}/notifications

# On new notification, receive:
# { notificationId, type, title, summary, createdAt }
```

Frontend: `useNotificationWebSocket` subscribes on login; dispatches `unreadCountChanged` event for layout badge.

## Producer integration checklist

- [ ] FR-04 booking CONFIRMED → `BOOKING_CONFIRMED` or `NOTIFICATION_DISPATCH`
- [ ] FR-10 contract PDF ready → `NOTIFICATION_DISPATCH` CONTRACT_GENERATED
- [ ] FR-12 bank transfer pending → `PAYMENT_PENDING_VERIFICATION` to Manager
- [ ] FR-12 payment confirmed → `PAYMENT_CONFIRMED` to Customer
- [ ] FR-13 maintenance status → `MAINTENANCE_STATUS_CHANGED`
- [ ] FR-05 calendar → WS `CALENDAR_REFRESH` (optional AVAILABILITY_UPDATED notification)

## Troubleshooting

| Symptom | Check |
|---------|-------|
| No notifications after event | `outbox_events` status PENDING; worker logs; V031 applied |
| Duplicate notifications | `dedupe_key` in payload; unique index |
| WebSocket no push | JWT on CONNECT; userId in topic matches token subject |
| Unread count wrong | Mark-read transaction; index `idx_notifications_user_unread` |
| Admin retry fails | Event must be FAILED status |
| Manager /notifications 404 | Add route in App.tsx (phase J) |

## Acceptance checklist (SC-001–SC-008)

- [ ] SC-001: Outbox → notification < 30s (95%)
- [ ] SC-002: User A cannot read user B notifications
- [ ] SC-003: Mark-read updates count < 2s
- [ ] SC-004: WS push < 5s when online
- [ ] SC-005: Replay produces 0 duplicates
- [ ] SC-006: FAILED events visible in Admin tab
- [ ] SC-007: Mark-all-read < 3s for ≤100 items
- [ ] SC-008: Deep link navigates in 1 click per type
