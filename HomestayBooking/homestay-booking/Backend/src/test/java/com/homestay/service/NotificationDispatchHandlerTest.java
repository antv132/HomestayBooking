package com.homestay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.homestay.entity.Notification;
import com.homestay.entity.OutboxEvent;
import com.homestay.entity.User;
import com.homestay.repository.NotificationRepository;
import com.homestay.repository.UserRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchHandlerTest {

    @Mock NotificationRepository notificationRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationWebSocketPublisher webSocketPublisher;
    @Mock ActivityLogService activityLogService;

    private NotificationDispatchHandler handler;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private User customer;

    @BeforeEach
    void setUp() {
        handler = new NotificationDispatchHandler(
                notificationRepository, userRepository, webSocketPublisher, activityLogService);
        customer = TestFixtures.user(User.Role.CUSTOMER);
        customer.setStatus(User.Status.ACTIVE);
    }

    @Test
    void dispatch_createsNotification_andPushesWs() throws Exception {
        UUID bookingId = UUID.randomUUID();
        String dedupe = "BOOKING_CONFIRMED:" + customer.getId() + ":" + bookingId;
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(notificationRepository.existsByDedupeKey(dedupe)).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("userId", customer.getId().toString());
        payload.put("type", "BOOKING_CONFIRMED");
        payload.put("title", "Confirmed");
        payload.put("content", "OK");
        payload.put("relatedEntityId", bookingId.toString());
        payload.put("relatedEntityType", "Booking");
        payload.put("dedupeKey", dedupe);

        OutboxEvent event = new OutboxEvent();
        event.setEventType(OutboxEvent.TYPE_NOTIFICATION_DISPATCH);
        handler.handle(event, payload);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(Notification.Type.BOOKING_CONFIRMED, captor.getValue().getType());
        assertEquals(dedupe, captor.getValue().getDedupeKey());
        verify(webSocketPublisher).publishToUser(any(), any());
    }

    @Test
    void dispatch_skipsDuplicateDedupeKey() throws Exception {
        String dedupe = "BOOKING_CONFIRMED:" + customer.getId() + ":x";
        when(notificationRepository.existsByDedupeKey(dedupe)).thenReturn(true);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("userId", customer.getId().toString());
        payload.put("type", "BOOKING_CONFIRMED");
        payload.put("title", "Confirmed");
        payload.put("content", "OK");
        payload.put("dedupeKey", dedupe);

        OutboxEvent event = new OutboxEvent();
        event.setEventType(OutboxEvent.TYPE_NOTIFICATION_DISPATCH);
        handler.handle(event, payload);

        verify(notificationRepository, never()).save(any());
        verify(webSocketPublisher, never()).publishToUser(any(), any());
    }

    @Test
    void dispatch_skipsInactiveUser() throws Exception {
        customer.setStatus(User.Status.SUSPENDED);
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(notificationRepository.existsByDedupeKey(any())).thenReturn(false);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("userId", customer.getId().toString());
        payload.put("type", "SYSTEM");
        payload.put("title", "Hi");
        payload.put("content", "x");
        payload.put("dedupeKey", "SYSTEM:" + customer.getId() + ":none");

        OutboxEvent event = new OutboxEvent();
        event.setEventType(OutboxEvent.TYPE_NOTIFICATION_DISPATCH);
        handler.handle(event, payload);

        verify(notificationRepository, never()).save(any());
    }
}
