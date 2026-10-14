package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.entity.Notification;
import com.homestay.entity.User;
import com.homestay.service.NotificationService;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock NotificationService notificationService;

    private NotificationController controller;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new NotificationController(notificationService);
        user = TestFixtures.user(User.Role.CUSTOMER);
    }

    @Test
    void getNotifications_ok() {
        when(notificationService.getNotifications(user.getId(), 0, 10, false))
                .thenReturn(Map.of("totalElements", 0));
        ResponseEntity<ApiResponse<Map<String, Object>>> res =
                controller.getNotifications(user, 0, 10, false);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void unreadCount_ok() {
        when(notificationService.getUnreadCount(user.getId())).thenReturn(3L);
        assertEquals(3L, controller.getUnreadCount(user).getBody().getData().get("count"));
    }

    @Test
    void markAllRead_ok() {
        when(notificationService.markAllRead(user.getId())).thenReturn(2);
        assertEquals(2, controller.markAllRead(user).getBody().getData().get("updated"));
    }

    @Test
    void getDetail_ok() {
        UUID id = UUID.randomUUID();
        when(notificationService.getNotificationDetail(id, user.getId()))
                .thenReturn(Map.of("id", id.toString()));
        assertEquals(HttpStatus.OK, controller.getNotificationDetail(id, user).getStatusCode());
    }

    @Test
    void delete_ok() {
        UUID id = UUID.randomUUID();
        assertEquals(HttpStatus.OK, controller.deleteNotification(id).getStatusCode());
        verify(notificationService).deleteNotification(id);
    }

    @Test
    void testSeed_ok() {
        assertEquals(HttpStatus.OK, controller.testSeed(user).getStatusCode());
        verify(notificationService, times(4)).sendNotification(
                eq(user.getId()), any(Notification.Type.class), any(), any(), any(), any());
    }
}
