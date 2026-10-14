package com.homestay.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.homestay.entity.Notification;
import com.homestay.entity.OutboxEvent;
import com.homestay.entity.User;
import com.homestay.repository.NotificationRepository;
import com.homestay.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * FR-15 — map outbox notification events → Notification rows (+ WebSocket push).
 */
@Service
public class NotificationDispatchHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchHandler.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationWebSocketPublisher webSocketPublisher;
    private final ActivityLogService activityLogService;

    public NotificationDispatchHandler(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            NotificationWebSocketPublisher webSocketPublisher,
            ActivityLogService activityLogService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.webSocketPublisher = webSocketPublisher;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public void handle(OutboxEvent event, JsonNode payload) {
        String eventType = event.getEventType();
        if (OutboxEvent.TYPE_NOTIFICATION_DISPATCH.equals(eventType)
                || OutboxEvent.TYPE_BOOKING_CONFIRMED.equals(eventType)
                || OutboxEvent.TYPE_CONTRACT_GENERATED.equals(eventType)
                || OutboxEvent.TYPE_PAYMENT_CONFIRMED.equals(eventType)
                || OutboxEvent.TYPE_PAYMENT_PENDING_VERIFICATION.equals(eventType)
                || OutboxEvent.TYPE_AVAILABILITY_UPDATED.equals(eventType)
                || OutboxEvent.TYPE_SYSTEM_BROADCAST.equals(eventType)) {
            dispatchFromPayload(payload);
            return;
        }
        if (OutboxEvent.TYPE_MAINTENANCE_STATUS_CHANGED.equals(eventType)) {
            mapMaintenanceStatusChanged(payload);
            return;
        }
        throw new IllegalArgumentException("Unsupported notification outbox type: " + eventType);
    }

    private void mapMaintenanceStatusChanged(JsonNode payload) {
        UUID customerId = UUID.fromString(payload.get("customerId").asText());
        UUID ticketId = UUID.fromString(payload.get("ticketId").asText());
        String newStatus = payload.hasNonNull("newStatus") ? payload.get("newStatus").asText() : "UPDATED";
        String roomNumber = payload.hasNonNull("roomNumber") ? payload.get("roomNumber").asText() : "";
        String title = "Cập nhật bảo trì";
        String content = "Ticket bảo trì" + (roomNumber.isBlank() ? "" : " phòng " + roomNumber)
                + " đã chuyển sang " + newStatus + ".";
        String dedupe = "MAINTENANCE_UPDATED:" + customerId + ":" + ticketId + ":" + newStatus;
        createIfAbsent(customerId, Notification.Type.MAINTENANCE_UPDATED, title, content,
                ticketId, "MaintenanceTicket", dedupe);
    }

    private void dispatchFromPayload(JsonNode payload) {
        UUID userId = UUID.fromString(payload.get("userId").asText());
        Notification.Type type = Notification.Type.valueOf(payload.get("type").asText());
        String title = payload.get("title").asText();
        String content = payload.hasNonNull("content") ? payload.get("content").asText() : "";
        UUID relatedEntityId = payload.hasNonNull("relatedEntityId")
                ? UUID.fromString(payload.get("relatedEntityId").asText()) : null;
        String relatedEntityType = payload.hasNonNull("relatedEntityType")
                ? payload.get("relatedEntityType").asText() : null;
        String dedupeKey = payload.hasNonNull("dedupeKey")
                ? payload.get("dedupeKey").asText()
                : NotificationOutboxPublisher.buildDedupeKey(type, userId, relatedEntityId);
        createIfAbsent(userId, type, title, content, relatedEntityId, relatedEntityType, dedupeKey);
    }

    private void createIfAbsent(
            UUID userId,
            Notification.Type type,
            String title,
            String content,
            UUID relatedEntityId,
            String relatedEntityType,
            String dedupeKey) {
        if (dedupeKey != null && notificationRepository.existsByDedupeKey(dedupeKey)) {
            log.info("Skip duplicate notification dedupeKey={}", dedupeKey);
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getStatus() != User.Status.ACTIVE) {
            log.info("Skip notification for inactive/missing user {}", userId);
            return;
        }

        Notification notif = new Notification();
        notif.setUser(user);
        notif.setType(type);
        notif.setTitle(title);
        notif.setContent(content);
        notif.setIsRead(false);
        notif.setRelatedEntityId(relatedEntityId);
        notif.setRelatedEntityType(relatedEntityType);
        notif.setDedupeKey(dedupeKey);

        try {
            Notification saved = notificationRepository.save(notif);
            activityLogService.log(null, ActivityLogService.NOTIFICATION_CREATED, "Notification",
                    saved.getId(), type.name() + " → " + userId);
            webSocketPublisher.publishToUser(userId, saved);
        } catch (DataIntegrityViolationException ex) {
            log.info("Idempotent skip on unique violation dedupeKey={}", dedupeKey);
        }
    }
}
