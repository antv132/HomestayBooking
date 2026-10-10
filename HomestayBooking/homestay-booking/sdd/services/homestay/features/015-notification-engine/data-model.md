# Data Model: FR-15 Notification Engine (Event-Driven)

**Date**: 2026-06-27  
**Spec**: [spec.md](./spec.md) | **Base**: `docs/Specification_v2.md` §5 Notification, OutboxEvent

## Scope

FR-15 **owns** `notifications` table. **Reads/writes** shared `outbox_events` (FR-04) for notification worker path only. **Consumes** FR-01 `users` (active check). **Does not own** booking/payment/maintenance/contract entities — references via `related_entity_id` + `related_entity_type`.

## ERD

```text
User 1──* Notification

OutboxEvent (shared) ──worker──> Notification 1──push──> WebSocket /topic/users/{userId}/notifications

Producer (FR-04/10/12/13) ──same TX──> OutboxEvent (PENDING)
```

## Table: notifications

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| user_id | UUID | FK users NOT NULL | Receiver |
| title | VARCHAR(255) | NOT NULL | |
| content | TEXT | NOT NULL | Full text in detail |
| type | VARCHAR(64) | NOT NULL | NotificationType enum |
| related_entity_id | UUID | nullable | Deep link target |
| related_entity_type | VARCHAR(64) | nullable | BOOKING, CONTRACT, PAYMENT, MAINTENANCE_TICKET, etc. |
| is_read | BOOLEAN | NOT NULL DEFAULT false | |
| dedupe_key | VARCHAR(128) | nullable | Idempotency |
| created_at | TIMESTAMPTZ | NOT NULL | |

**Indexes**:

- `(user_id, created_at DESC)` — SCR-13 list
- `(user_id, is_read)` WHERE `is_read = false` — unread count
- `UNIQUE (user_id, type, related_entity_id, dedupe_key)` WHERE `related_entity_id IS NOT NULL AND dedupe_key IS NOT NULL` — idempotency

### NotificationType enum

`BOOKING_CONFIRMED` | `CONTRACT_GENERATED` | `PAYMENT_CONFIRMED` | `PAYMENT_PENDING_VERIFICATION` | `MAINTENANCE_UPDATED` | `AVAILABILITY_UPDATED` | `SYSTEM`

### RelatedEntityType enum (reference)

`BOOKING` | `CONTRACT` | `PAYMENT` | `MAINTENANCE_TICKET` | `ROOM` | `PROPERTY` | `null` (SYSTEM)

## Table: outbox_events (shared — FR-04 owned schema)

FR-15 worker filters `event_type IN`:

- `NOTIFICATION_DISPATCH`
- `BOOKING_CONFIRMED`
- `CONTRACT_GENERATED`
- `PAYMENT_PENDING_VERIFICATION`
- `PAYMENT_CONFIRMED`
- `MAINTENANCE_STATUS_CHANGED`
- `SYSTEM_BROADCAST`

Existing columns per FR-004 `data-model.md`: `id`, `event_type`, `payload` JSONB, `status`, `retry_count`, `created_at`, `processed_at`.

### NOTIFICATION_DISPATCH payload schema

```json
{
  "userId": "uuid",
  "type": "BOOKING_CONFIRMED",
  "title": "Booking confirmed",
  "content": "Your booking #12345 has been confirmed.",
  "relatedEntityType": "BOOKING",
  "relatedEntityId": "uuid",
  "dedupeKey": "BOOKING_CONFIRMED:uuid:uuid"
}
```

### MAINTENANCE_STATUS_CHANGED payload (FR-13)

```json
{
  "ticketId": "uuid",
  "customerId": "uuid",
  "previousStatus": "OPEN",
  "newStatus": "IN_PROGRESS",
  "roomNumber": "101"
}
```

Worker maps → `MAINTENANCE_UPDATED` notification to `customerId`.

## Outbox status transitions (notification worker)

```text
PENDING ──success──> PROCESSED
PENDING ──failure──> PENDING (retry_count++) 
PENDING ──retry_count >= 3──> FAILED
FAILED ──admin retry──> PENDING
```

## Validation Rules (application layer)

| Rule | Error code (suggested) |
|------|------------------------|
| Notification not owned by user | `UNAUTHORIZED` |
| Notification id not found | `NOT_FOUND` |
| Invalid page/size params | `VALIDATION_ERROR` |
| Admin retry non-FAILED outbox | `INVALID_STATUS` |
| Duplicate dedupe insert | Silent skip (idempotent) |

## ActivityLog actions

`NOTIFICATION_CREATED`, `NOTIFICATION_MARKED_READ`, `NOTIFICATION_MARKED_ALL_READ`, `OUTBOX_PROCESSED`, `OUTBOX_FAILED`, `OUTBOX_RETRY_REQUESTED`

## WebSocket message shapes

### Notification push (`/topic/users/{userId}/notifications`)

```json
{
  "notificationId": "uuid",
  "type": "PAYMENT_CONFIRMED",
  "title": "Payment received",
  "summary": "Your payment of 1,500,000 VND was confirmed.",
  "createdAt": "2026-06-27T10:00:00Z"
}
```

### Calendar refresh (`/topic/properties/{propertyId}/calendar`) — FR-05, no DB row

```json
{
  "type": "CALENDAR_REFRESH",
  "propertyId": "uuid",
  "roomId": "uuid",
  "date": "2026-06-28"
}
```

## Flyway

`V031__notifications_fr15.sql`:

```sql
CREATE TABLE notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id),
  title VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  type VARCHAR(64) NOT NULL,
  related_entity_id UUID,
  related_entity_type VARCHAR(64),
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  dedupe_key VARCHAR(128),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id) WHERE is_read = FALSE;

CREATE UNIQUE INDEX uq_notifications_dedupe
  ON notifications (user_id, type, related_entity_id, dedupe_key)
  WHERE related_entity_id IS NOT NULL AND dedupe_key IS NOT NULL;
```

**Seed** (dev quickstart): optional SQL insert `NOTIFICATION_DISPATCH` outbox + sample notifications for demo user.

## API DTO mapping

| Entity field | API response field |
|--------------|-------------------|
| content | `content` (api-spec SCR-13 uses `message` — DTO aliases both or standardize on `content`) |
| is_read | `isRead` |
| related_entity_id | `relatedEntityId` |
| related_entity_type | `relatedEntityType` |
| created_at | `createdAt` |

## Cross-FR references

| FR | Interaction |
|----|-------------|
| FR-04 | Emits `BOOKING_CONFIRMED`; owns `outbox_events` DDL |
| FR-10 | Emits `CONTRACT_GENERATED` in-app after PDF; PDF worker separate |
| FR-12 | Emits payment notification events |
| FR-13 | Emits `MAINTENANCE_STATUS_CHANGED` |
| FR-05 | Subscribes calendar WS topic; may trigger `AVAILABILITY_UPDATED` notification on material status change only |
| FR-01 | JWT userId scopes all inbox queries |
