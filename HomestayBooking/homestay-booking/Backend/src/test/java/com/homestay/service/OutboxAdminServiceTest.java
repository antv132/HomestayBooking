package com.homestay.service;

import com.homestay.dto.response.PageResponse;
import com.homestay.entity.OutboxEvent;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.OutboxEventRepository;
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
class OutboxAdminServiceTest {

    @Mock OutboxEventRepository outboxEventRepository;
    @Mock ActivityLogService activityLogService;

    private OutboxAdminService outboxAdminService;
    private User admin;
    private OutboxEvent outboxEvent;

    @BeforeEach
    void setUp() {
        outboxAdminService = new OutboxAdminService(
                outboxEventRepository,
                activityLogService);

        admin = TestFixtures.user(User.Role.ADMIN);

        outboxEvent = new OutboxEvent();
        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setEventType(OutboxEvent.TYPE_NOTIFICATION_DISPATCH);
        outboxEvent.setStatus(OutboxEvent.Status.FAILED);
        outboxEvent.setRetryCount(3);
        outboxEvent.setLastError("Connection timeout");
        outboxEvent.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void list_returnsPagedOutboxEvents() {
        Page<OutboxEvent> page = new PageImpl<>(List.of(outboxEvent));
        when(outboxEventRepository.findByStatusAndEventTypeIn(
                eq(OutboxEvent.Status.FAILED),
                eq(OutboxAdminService.NOTIFICATION_EVENT_TYPES),
                any(Pageable.class)))
                .thenReturn(page);

        PageResponse<Map<String, Object>> result = outboxAdminService.list(
                OutboxEvent.Status.FAILED, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        Map<String, Object> item = result.getContent().get(0);
        assertEquals(outboxEvent.getId().toString(), item.get("id"));
        assertEquals(outboxEvent.getEventType(), item.get("eventType"));
        assertEquals("FAILED", item.get("status"));
        assertEquals(3, item.get("retryCount"));
    }

    @Test
    void retry_resetsStatusToPending() {
        when(outboxEventRepository.findById(outboxEvent.getId())).thenReturn(Optional.of(outboxEvent));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(outboxEvent);

        Map<String, Object> result = outboxAdminService.retry(outboxEvent.getId(), admin);

        assertNotNull(result);
        assertEquals(OutboxEvent.Status.PENDING, outboxEvent.getStatus());
        assertNull(outboxEvent.getLastError());
        verify(outboxEventRepository).save(outboxEvent);
        verify(activityLogService).log(eq(admin), eq(ActivityLogService.OUTBOX_RETRY_REQUESTED),
                eq("OutboxEvent"), eq(outboxEvent.getId()), anyString());
    }

    @Test
    void retry_notFound_throws() {
        when(outboxEventRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> outboxAdminService.retry(UUID.randomUUID(), admin));
    }

    @Test
    void retry_nonNotificationEvent_throws() {
        outboxEvent.setEventType("SOME_OTHER_TYPE");
        when(outboxEventRepository.findById(outboxEvent.getId())).thenReturn(Optional.of(outboxEvent));

        assertThrows(BusinessException.class,
                () -> outboxAdminService.retry(outboxEvent.getId(), admin));
    }

    @Test
    void retry_notFailedStatus_throws() {
        outboxEvent.setStatus(OutboxEvent.Status.PROCESSED);
        when(outboxEventRepository.findById(outboxEvent.getId())).thenReturn(Optional.of(outboxEvent));

        assertThrows(BusinessException.class,
                () -> outboxAdminService.retry(outboxEvent.getId(), admin));
    }

    @Test
    void retry_pendingStatus_throws() {
        outboxEvent.setStatus(OutboxEvent.Status.PENDING);
        when(outboxEventRepository.findById(outboxEvent.getId())).thenReturn(Optional.of(outboxEvent));

        assertThrows(BusinessException.class,
                () -> outboxAdminService.retry(outboxEvent.getId(), admin));
    }
}
