package com.homestay.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay.entity.OutboxEvent;
import com.homestay.entity.User;
import com.homestay.repository.OutboxEventRepository;
import com.homestay.repository.UserRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorServiceTest {

    @Mock OutboxEventRepository outboxEventRepository;
    @Mock ContractService contractService;
    @Mock NotificationDispatchHandler notificationDispatchHandler;
    @Mock UserRepository userRepository;
    @Mock ActivityLogService activityLogService;
    @Mock ObjectProvider<OutboxProcessorService> selfProvider;

    private OutboxProcessorService outboxProcessorService;
    private ObjectMapper objectMapper;
    private OutboxEvent contractEvent;
    private OutboxEvent notificationEvent;
    private User actor;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        when(selfProvider.getIfAvailable()).thenReturn(null);
        outboxProcessorService = new OutboxProcessorService(
                outboxEventRepository,
                contractService,
                notificationDispatchHandler,
                userRepository,
                objectMapper,
                activityLogService,
                selfProvider);

        java.lang.reflect.Field batchSizeField = OutboxProcessorService.class.getDeclaredField("batchSize");
        batchSizeField.setAccessible(true);
        batchSizeField.setInt(outboxProcessorService, 20);

        java.lang.reflect.Field maxRetriesField = OutboxProcessorService.class.getDeclaredField("maxRetries");
        maxRetriesField.setAccessible(true);
        maxRetriesField.setInt(outboxProcessorService, 3);

        actor = TestFixtures.user(User.Role.MANAGER);

        contractEvent = new OutboxEvent();
        contractEvent.setId(UUID.randomUUID());
        contractEvent.setEventType(OutboxEvent.TYPE_CONTRACT_GENERATE);
        contractEvent.setStatus(OutboxEvent.Status.PENDING);
        contractEvent.setPayload("{\"bookingId\":\"" + UUID.randomUUID() + "\",\"actorUserId\":\"" + actor.getId() + "\"}");
        contractEvent.setRetryCount(0);
        contractEvent.setCreatedAt(LocalDateTime.now());

        notificationEvent = new OutboxEvent();
        notificationEvent.setId(UUID.randomUUID());
        notificationEvent.setEventType(OutboxEvent.TYPE_NOTIFICATION_DISPATCH);
        notificationEvent.setStatus(OutboxEvent.Status.PENDING);
        notificationEvent.setPayload("{\"userId\":\"" + UUID.randomUUID() + "\",\"type\":\"BOOKING_CONFIRMATION\"}");
        notificationEvent.setRetryCount(0);
        notificationEvent.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void processPendingBatch_processesContractGenerate() throws Exception {
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                eq(OutboxEvent.Status.PENDING), any(PageRequest.class)))
                .thenReturn(List.of(contractEvent));
        when(outboxEventRepository.findById(contractEvent.getId())).thenReturn(Optional.of(contractEvent));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(contractEvent);
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));

        int processed = outboxProcessorService.processPendingBatch();

        assertEquals(1, processed);
        verify(contractService).processContractGenerateFromOutbox(any(UUID.class), eq(actor));
        verify(outboxEventRepository).save(argThat(e ->
                e.getStatus() == OutboxEvent.Status.PROCESSED && e.getProcessedAt() != null));
        verify(activityLogService).log(eq(null), eq(ActivityLogService.OUTBOX_PROCESSED),
                eq("OutboxEvent"), eq(contractEvent.getId()), anyString());
    }

    @Test
    void processPendingBatch_processesNotificationDispatch() throws Exception {
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                eq(OutboxEvent.Status.PENDING), any(PageRequest.class)))
                .thenReturn(List.of(notificationEvent));
        when(outboxEventRepository.findById(notificationEvent.getId())).thenReturn(Optional.of(notificationEvent));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(notificationEvent);

        int processed = outboxProcessorService.processPendingBatch();

        assertEquals(1, processed);
        verify(notificationDispatchHandler).handle(eq(notificationEvent), any(JsonNode.class));
        verify(outboxEventRepository).save(argThat(e -> e.getStatus() == OutboxEvent.Status.PROCESSED));
    }

    @Test
    void processPendingBatch_handlesFailureWithRetry() throws Exception {
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                eq(OutboxEvent.Status.PENDING), any(PageRequest.class)))
                .thenReturn(List.of(contractEvent));
        when(outboxEventRepository.findById(contractEvent.getId())).thenReturn(Optional.of(contractEvent));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(contractEvent);
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));
        doThrow(new RuntimeException("Processing failed"))
                .when(contractService).processContractGenerateFromOutbox(any(), any());

        int processed = outboxProcessorService.processPendingBatch();

        assertEquals(0, processed);
        verify(outboxEventRepository).save(argThat(e ->
                e.getRetryCount() == 1 && e.getStatus() == OutboxEvent.Status.PENDING));
    }

    @Test
    void processPendingBatch_marksFailedAfterMaxRetries() throws Exception {
        contractEvent.setRetryCount(2);
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                eq(OutboxEvent.Status.PENDING), any(PageRequest.class)))
                .thenReturn(List.of(contractEvent));
        when(outboxEventRepository.findById(contractEvent.getId())).thenReturn(Optional.of(contractEvent));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(contractEvent);
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));
        doThrow(new RuntimeException("Processing failed"))
                .when(contractService).processContractGenerateFromOutbox(any(), any());

        outboxProcessorService.processPendingBatch();

        verify(outboxEventRepository).save(argThat(e -> e.getStatus() == OutboxEvent.Status.FAILED));
        verify(activityLogService).log(eq(null), eq(ActivityLogService.OUTBOX_FAILED),
                eq("OutboxEvent"), eq(contractEvent.getId()), anyString());
    }

    @Test
    void processPendingBatch_emptyBatch_returnsZero() {
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                eq(OutboxEvent.Status.PENDING), any(PageRequest.class)))
                .thenReturn(List.of());

        int processed = outboxProcessorService.processPendingBatch();

        assertEquals(0, processed);
        verify(contractService, never()).processContractGenerateFromOutbox(any(), any());
    }

    @Test
    void processPendingBatch_unknownEventType_throws() {
        OutboxEvent unknown = new OutboxEvent();
        unknown.setId(UUID.randomUUID());
        unknown.setEventType("UNKNOWN_TYPE");
        unknown.setStatus(OutboxEvent.Status.PENDING);
        unknown.setPayload("{}");
        unknown.setRetryCount(0);
        unknown.setCreatedAt(LocalDateTime.now());

        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                eq(OutboxEvent.Status.PENDING), any(PageRequest.class)))
                .thenReturn(List.of(unknown));
        when(outboxEventRepository.findById(unknown.getId())).thenReturn(Optional.of(unknown));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(unknown);

        int processed = outboxProcessorService.processPendingBatch();

        assertEquals(0, processed);
        verify(outboxEventRepository).save(argThat(e -> e.getRetryCount() == 1));
    }
}
