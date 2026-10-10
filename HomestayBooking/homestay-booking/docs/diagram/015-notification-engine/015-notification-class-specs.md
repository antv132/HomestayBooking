# 015 - Notification Engine: Class Specifications

[Provide the description for each class and the methods in each class, following the table format as below]

## 1. Entity

### Notification Class
Represents a notification sent to a user.
| No | Method | Description |
|---|---|---|
| - | - | (Entity class. Contains `user_id`, `type`, `title`, `content`, `is_read`, `related_entity_id`, `related_entity_type`, `dedupe_key`.) |

---

## 2. Notification Pipeline & Transactional Outbox (FR-15)

### NotificationService Class
Handles enqueueing notifications.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | sendNotification | Called by any Domain Service (e.g., `BookingService`). Calls `NotificationOutboxPublisher.enqueueDispatch`. Inserts an `OutboxEvent` row in the SAME database transaction as the domain change. Returns immediately. |

### OutboxProcessorService Class
Background worker for processing outbox events.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | processOutbox | Runs on a background thread (scheduler). Polls `outbox_events` table for `PENDING` events. Returns void. |

### NotificationDispatchHandler Class
Processes notification dispatch events.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | handleDispatch | Processes `TYPE_NOTIFICATION_DISPATCH` events. Checks `dedupe_key` to avoid duplicate processing. Creates the actual `Notification` entity row. Calls `NotificationWebSocketPublisher`. Returns void. |

### NotificationWebSocketPublisher Class
Pushes real-time notifications via WebSockets.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | pushNotification | Uses STOMP over WebSockets. Pushes real-time JSON payload to the specific user's topic: `/topic/users/{userId}/notifications`. Returns void. |

---

## 3. Controllers

### NotificationV1Controller Class
REST API endpoints for managing notifications.
[Provide the detailed description for the class methods]
| No | Method | Description |
|---|---|---|
| 01 | getNotifications | GET `/`. Fetches notifications for the `currentUser`. Supports pagination and `unreadOnly` filter. Returns 200 OK. |
| 02 | getUnreadCount | GET `/unread-count`. Fast count of unread messages for the UI badge. Returns 200 OK. |
| 03 | markAllRead | PATCH `/mark-all-read` (and POST `/read-all`). Marks all notifications for the user as `is_read = true`. Returns 200 OK. |
| 04 | getNotificationDetail | GET `/{id}`. Fetches details and auto-marks as read. Returns 200 OK. |
| 05 | markAsRead | PATCH `/{id}/read` (and POST `/{id}/read`). Explicitly marks a single notification as read. Returns 200 OK. |
