package com.homestay.service;

import com.homestay.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** FR-15 — STOMP push after notification create. */
@Service
public class NotificationWebSocketPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationWebSocketPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishToUser(UUID userId, Notification notification) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("notificationId", notification.getId().toString());
        body.put("type", notification.getType().name());
        body.put("title", notification.getTitle());
        body.put("isRead", notification.getIsRead());
        body.put("relatedEntityId",
                notification.getRelatedEntityId() != null
                        ? notification.getRelatedEntityId().toString() : null);
        body.put("relatedEntityType", notification.getRelatedEntityType());
        body.put("createdAt",
                notification.getCreatedAt() != null ? notification.getCreatedAt().toString() : null);
        try {
            messagingTemplate.convertAndSend("/topic/users/" + userId + "/notifications", body);
        } catch (Exception e) {
            log.warn("WebSocket push failed for user {}: {}", userId, e.getMessage());
        }
    }

    public void publishCalendarRefresh(UUID propertyId, Map<String, Object> payload) {
        try {
            messagingTemplate.convertAndSend("/topic/properties/" + propertyId + "/calendar", payload);
        } catch (Exception e) {
            log.warn("Calendar WS push failed for property {}: {}", propertyId, e.getMessage());
        }
    }
}
