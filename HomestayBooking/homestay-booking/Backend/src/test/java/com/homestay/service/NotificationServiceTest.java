package com.homestay.service;

import com.homestay.entity.Notification;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.NotificationRepository;
import com.homestay.repository.UserRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationOutboxPublisher notificationOutboxPublisher;
    @Mock ActivityLogService activityLogService;

    private NotificationService notificationService;
    private User user;
    private Notification notification;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                userRepository,
                notificationOutboxPublisher,
                activityLogService);

        user = TestFixtures.user(User.Role.CUSTOMER);
        notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUser(user);
        notification.setType(Notification.Type.BOOKING_CONFIRMED);
        notification.setTitle("Test Notification");
        notification.setContent("Test Content");
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void getNotifications_returnsPagedNotifications() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1L);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable))
                .thenReturn(page);

        Map<String, Object> result = notificationService.getNotifications(user.getId(), 0, 10, false);

        assertNotNull(result);
        assertEquals(0, result.get("page"));
        assertEquals(10, result.get("size")); // Page size, not content size
        assertEquals(1L, result.get("totalElements"));
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
    }

    @Test
    void getNotifications_unreadOnly_filtersUnread() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId(), pageable))
                .thenReturn(page);

        Map<String, Object> result = notificationService.getNotifications(user.getId(), 0, 10, true);

        assertNotNull(result);
        verify(notificationRepository).findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId(), pageable);
    }

    @Test
    void getNotifications_capsSizeAt50() {
        Pageable pageable = PageRequest.of(0, 50);
        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(user.getId()), any(Pageable.class)))
                .thenReturn(page);

        notificationService.getNotifications(user.getId(), 0, 100, false);

        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(eq(user.getId()), argThat(p -> p.getPageSize() == 50));
    }

    @Test
    void getUnreadCount_returnsCount() {
        when(notificationRepository.countByUserIdAndIsReadFalse(user.getId())).thenReturn(5L);

        long count = notificationService.getUnreadCount(user.getId());

        assertEquals(5L, count);
        verify(notificationRepository).countByUserIdAndIsReadFalse(user.getId());
    }

    @Test
    void markAllRead_updatesAndReturnsCount() {
        when(notificationRepository.markAllAsReadByUserId(user.getId())).thenReturn(3);

        int updated = notificationService.markAllRead(user.getId());

        assertEquals(3, updated);
        verify(notificationRepository).markAllAsReadByUserId(user.getId());
        verify(activityLogService).log(eq(null), eq(ActivityLogService.NOTIFICATION_MARKED_ALL_READ),
                eq("User"), eq(user.getId()), anyString());
    }

    @Test
    void getNotificationDetail_marksAsReadIfUnread() {
        notification.setIsRead(false);
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        Map<String, Object> result = notificationService.getNotificationDetail(notification.getId(), user.getId());

        assertNotNull(result);
        assertTrue(notification.getIsRead());
        verify(notificationRepository).save(notification);
        verify(activityLogService).log(eq(null), eq(ActivityLogService.NOTIFICATION_MARKED_READ),
                eq("Notification"), eq(notification.getId()), eq("via detail"));
    }

    @Test
    void getNotificationDetail_doesNotMarkIfAlreadyRead() {
        notification.setIsRead(true);
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        notificationService.getNotificationDetail(notification.getId(), user.getId());

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void getNotificationDetail_throwsIfNotOwned() {
        User otherUser = TestFixtures.user(User.Role.CUSTOMER);
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        assertThrows(BusinessException.class,
                () -> notificationService.getNotificationDetail(notification.getId(), otherUser.getId()));
    }

    @Test
    void markAsRead_updatesUnreadNotification() {
        notification.setIsRead(false);
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.markAsRead(notification.getId(), user.getId());

        assertTrue(notification.getIsRead());
        verify(notificationRepository).save(notification);
        verify(activityLogService).log(eq(null), eq(ActivityLogService.NOTIFICATION_MARKED_READ),
                eq("Notification"), eq(notification.getId()), eq("manual"));
    }

    @Test
    void deleteNotification_ownershipCheck_success() {
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        notificationService.deleteNotification(notification.getId(), user.getId());

        verify(notificationRepository).delete(notification);
    }

    @Test
    void deleteNotification_wrongOwner_throws() {
        User otherUser = TestFixtures.user(User.Role.CUSTOMER);
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        assertThrows(BusinessException.class,
                () -> notificationService.deleteNotification(notification.getId(), otherUser.getId()));
    }

    @Test
    void sendNotification_enqueuesOutboxEvent() {
        when(userRepository.existsById(user.getId())).thenReturn(true);

        notificationService.sendNotification(
                user.getId(),
                Notification.Type.BOOKING_CONFIRMED,
                "Title",
                "Content",
                UUID.randomUUID(),
                "Booking");

        verify(notificationOutboxPublisher).enqueueDispatch(
                eq(user.getId()),
                eq(Notification.Type.BOOKING_CONFIRMED),
                eq("Title"),
                eq("Content"),
                any(UUID.class),
                eq("Booking"),
                anyString());
    }

    @Test
    void sendNotification_userNotFound_throws() {
        when(userRepository.existsById(user.getId())).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.sendNotification(
                        user.getId(),
                        Notification.Type.BOOKING_CONFIRMED,
                        "Title",
                        "Content",
                        UUID.randomUUID(),
                        "Booking"));
    }
}
