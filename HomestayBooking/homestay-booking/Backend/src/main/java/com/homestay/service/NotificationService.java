package com.homestay.service;

import com.homestay.entity.Notification;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.NotificationRepository;
import com.homestay.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationOutboxPublisher notificationOutboxPublisher;
    private final ActivityLogService activityLogService;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               NotificationOutboxPublisher notificationOutboxPublisher,
                               ActivityLogService activityLogService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.notificationOutboxPublisher = notificationOutboxPublisher;
        this.activityLogService = activityLogService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getNotifications(UUID userId, int page, int size, boolean unreadOnly) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(page, safeSize);
        Page<Notification> result = unreadOnly
                ? notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<Map<String, Object>> content = result.getContent().stream()
                .map(this::notifToMap)
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", content);
        response.put("page", result.getNumber());
        response.put("size", result.getSize());
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        return response;
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public int markAllRead(UUID userId) {
        int updated = notificationRepository.markAllAsReadByUserId(userId);
        activityLogService.log(null, ActivityLogService.NOTIFICATION_MARKED_ALL_READ, "User",
                userId, "updated=" + updated);
        return updated;
    }

    @Transactional
    public Map<String, Object> getNotificationDetail(UUID notifId, UUID userId) {
        Notification notif = findOwnedNotification(notifId, userId);
        if (!notif.getIsRead()) {
            notif.setIsRead(true);
            notificationRepository.save(notif);
            activityLogService.log(null, ActivityLogService.NOTIFICATION_MARKED_READ, "Notification",
                    notifId, "via detail");
        }
        return notifToMap(notif);
    }

    @Transactional
    public Map<String, Object> markAsRead(UUID notifId, UUID userId) {
        Notification notif = findOwnedNotification(notifId, userId);
        if (!notif.getIsRead()) {
            notif.setIsRead(true);
            notificationRepository.save(notif);
            activityLogService.log(null, ActivityLogService.NOTIFICATION_MARKED_READ, "Notification",
                    notifId, "manual");
        }
        return notifToMap(notif);
    }

    @Transactional
    public void deleteNotification(UUID notifId, UUID actorUserId) {
        Notification notif = findOwnedNotification(notifId, actorUserId);
        notificationRepository.delete(notif);
    }

    /** @deprecated use ownership-aware delete */
    @Transactional
    public void deleteNotification(UUID notifId) {
        Notification notif = notificationRepository.findById(notifId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notificationRepository.delete(notif);
    }

    /**
     * Domain producers call this — FR-15 enqueues Outbox NOTIFICATION_DISPATCH (same TX).
     * Worker creates the Notification row asynchronously.
     */
    @Transactional
    public Notification sendNotification(UUID userId, Notification.Type type,
                                         String title, String content,
                                         UUID relatedEntityId, String relatedEntityType) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found for notification");
        }
        String dedupe = NotificationOutboxPublisher.buildDedupeKey(type, userId, relatedEntityId);
        notificationOutboxPublisher.enqueueDispatch(
                userId, type, title, content, relatedEntityId, relatedEntityType, dedupe);
        return null;
    }

    private Notification findOwnedNotification(UUID notifId, UUID userId) {
        Notification notif = notificationRepository.findById(notifId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo"));
        if (!notif.getUser().getId().equals(userId)) {
            throw new BusinessException("Bạn không có quyền xem thông báo này");
        }
        return notif;
    }

    private Map<String, Object> notifToMap(Notification n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId().toString());
        m.put("type", n.getType().name());
        m.put("title", n.getTitle());
        m.put("content", n.getContent());
        m.put("isRead", n.getIsRead());
        m.put("relatedEntityId", n.getRelatedEntityId() != null ? n.getRelatedEntityId().toString() : null);
        m.put("relatedEntityType", n.getRelatedEntityType());
        m.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
        return m;
    }
}
