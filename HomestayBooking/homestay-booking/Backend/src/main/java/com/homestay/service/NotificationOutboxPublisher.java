package com.homestay.service;

import com.homestay.entity.Notification;
import com.homestay.entity.OutboxEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * FR-15 — enqueue notification dispatch in the same TX as domain writes.
 */
@Service
public class NotificationOutboxPublisher {

    private final OutboxPublisher outboxPublisher;

    public NotificationOutboxPublisher(OutboxPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    @Transactional
    public OutboxEvent enqueueDispatch(
            UUID userId,
            Notification.Type type,
            String title,
            String content,
            UUID relatedEntityId,
            String relatedEntityType,
            String dedupeKey) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId.toString());
        payload.put("type", type.name());
        payload.put("title", title);
        payload.put("content", content != null ? content : "");
        if (relatedEntityId != null) {
            payload.put("relatedEntityId", relatedEntityId.toString());
        }
        if (relatedEntityType != null) {
            payload.put("relatedEntityType", relatedEntityType);
        }
        payload.put("dedupeKey", dedupeKey != null ? dedupeKey : buildDedupeKey(type, userId, relatedEntityId));
        return outboxPublisher.enqueue(OutboxEvent.TYPE_NOTIFICATION_DISPATCH, payload);
    }

    public static String buildDedupeKey(Notification.Type type, UUID userId, UUID relatedEntityId) {
        return type.name() + ":" + userId
                + ":" + (relatedEntityId != null ? relatedEntityId : "none");
    }
}
