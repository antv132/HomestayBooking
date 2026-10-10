# 015 - Notification Engine: Database Queries

## 1. Inbox Listing & Counting (User Facing)

### Fetch Notifications (Paginated)
```sql
-- NotificationRepository.findByUserIdOrderByCreatedAtDesc
SELECT n.*
FROM notifications n
WHERE n.user_id = :userId
ORDER BY n.created_at DESC
OFFSET :offset LIMIT :limit;
```

### Fetch Only Unread Notifications
```sql
-- NotificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc
SELECT n.*
FROM notifications n
WHERE n.user_id = :userId
  AND n.is_read = FALSE
ORDER BY n.created_at DESC
OFFSET :offset LIMIT :limit;
```

### Count Unread Notifications (Badge)
```sql
-- NotificationRepository.countByUserIdAndIsReadFalse
SELECT COUNT(1)
FROM notifications n
WHERE n.user_id = :userId
  AND n.is_read = FALSE;
```

---

## 2. Notification Updates

### Mark Single Notification as Read
```sql
-- NotificationRepository.save() via Service
UPDATE notifications
SET is_read = TRUE
WHERE id = :notificationId
  AND user_id = :userId;
```

### Mark All as Read (Bulk Action)
```sql
-- NotificationRepository.markAllAsReadByUserId
UPDATE notifications
SET is_read = TRUE
WHERE user_id = :userId
  AND is_read = FALSE;
```

---

## 3. The Transactional Outbox Pipeline

The following queries occur automatically in the background, bridging the gap between Domain logic and the Notification system.

### Domain TX: Insert Event to Outbox
```sql
-- Executed inside the Domain Transaction (e.g., when Booking is Confirmed)
INSERT INTO outbox_events (
    id, event_type, aggregate_type, aggregate_id, 
    payload, status, created_at
) VALUES (
    gen_random_uuid(), 
    'NOTIFICATION_DISPATCH', 
    'Notification', 
    :userId, 
    :jsonPayload, -- Contains type, title, content, dedupeKey
    'PENDING', 
    CURRENT_TIMESTAMP
);
```

### Worker: Read Pending Events
```sql
-- OutboxProcessorService polling loop
SELECT * 
FROM outbox_events 
WHERE status = 'PENDING' 
ORDER BY created_at ASC 
LIMIT 100;
```

### Dispatch Handler: Idempotent Notification Creation
```sql
-- 1. Check Dedupe Key
SELECT COUNT(1) > 0 
FROM notifications 
WHERE dedupe_key = :dedupeKey;

-- 2. Insert if not exists
INSERT INTO notifications (
    id, user_id, type, title, content, 
    is_read, related_entity_id, related_entity_type, 
    dedupe_key, created_at
) VALUES (
    gen_random_uuid(), :userId, :type, :title, :content,
    FALSE, :relatedEntityId, :relatedEntityType,
    :dedupeKey, CURRENT_TIMESTAMP
);
```

### Worker: Mark Outbox Processed
```sql
UPDATE outbox_events
SET status = 'PROCESSED',
    processed_at = CURRENT_TIMESTAMP
WHERE id = :eventId;
```
