package com.homestay.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay.entity.OutboxEvent;
import com.homestay.entity.User;
import com.homestay.repository.OutboxEventRepository;
import com.homestay.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class OutboxProcessorService {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessorService.class);

    private static final Set<String> NOTIFICATION_EVENT_TYPES = Set.of(
            OutboxEvent.TYPE_NOTIFICATION_DISPATCH,
            OutboxEvent.TYPE_BOOKING_CONFIRMED,
            OutboxEvent.TYPE_CONTRACT_GENERATED,
            OutboxEvent.TYPE_PAYMENT_PENDING_VERIFICATION,
            OutboxEvent.TYPE_PAYMENT_CONFIRMED,
            OutboxEvent.TYPE_MAINTENANCE_STATUS_CHANGED,
            OutboxEvent.TYPE_AVAILABILITY_UPDATED,
            OutboxEvent.TYPE_SYSTEM_BROADCAST
    );

    private final OutboxEventRepository outboxEventRepository;
    private final ContractService contractService;
    private final NotificationDispatchHandler notificationDispatchHandler;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final ActivityLogService activityLogService;
    /** Lazily resolve self-proxy so {@code REQUIRES_NEW} applies (null in plain unit tests). */
    private final ObjectProvider<OutboxProcessorService> selfProvider;

    @Value("${app.outbox.batch-size:20}")
    private int batchSize;

    @Value("${app.outbox.max-retries:3}")
    private int maxRetries;

    public OutboxProcessorService(OutboxEventRepository outboxEventRepository,
                                  ContractService contractService,
                                  NotificationDispatchHandler notificationDispatchHandler,
                                  UserRepository userRepository,
                                  ObjectMapper objectMapper,
                                  ActivityLogService activityLogService,
                                  ObjectProvider<OutboxProcessorService> selfProvider) {
        this.outboxEventRepository = outboxEventRepository;
        this.contractService = contractService;
        this.notificationDispatchHandler = notificationDispatchHandler;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.activityLogService = activityLogService;
        this.selfProvider = selfProvider;
    }

    /**
     * No outer transaction — each event runs in its own TX so one failure cannot
     * mark the whole batch rollback-only ({@code UnexpectedRollbackException}).
     */
    public int processPendingBatch() {
        List<OutboxEvent> batch = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEvent.Status.PENDING, PageRequest.of(0, batchSize));
        OutboxProcessorService worker = selfProvider.getIfAvailable();
        if (worker == null) {
            worker = this;
        }
        int processed = 0;
        for (OutboxEvent event : batch) {
            try {
                if (worker.processOneSuccess(event.getId())) {
                    processed++;
                }
            } catch (Exception e) {
                worker.recordFailure(event.getId(), e);
            }
        }
        return processed;
    }

    /** Process one PENDING event; commits on success. Failure rolls back this TX only. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processOneSuccess(UUID eventId) throws Exception {
        OutboxEvent event = outboxEventRepository.findById(eventId).orElse(null);
        if (event == null || event.getStatus() != OutboxEvent.Status.PENDING) {
            return false;
        }
        dispatch(event);
        event.setStatus(OutboxEvent.Status.PROCESSED);
        event.setProcessedAt(LocalDateTime.now());
        event.setLastError(null);
        outboxEventRepository.save(event);
        activityLogService.log(null, ActivityLogService.OUTBOX_PROCESSED, "OutboxEvent",
                event.getId(), event.getEventType());
        return true;
    }

    /** Separate TX — persist retry/FAILED after success-path TX rolled back. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID eventId, Exception error) {
        OutboxEvent event = outboxEventRepository.findById(eventId).orElse(null);
        if (event == null) {
            return;
        }
        event.setRetryCount(event.getRetryCount() + 1);
        event.setLastError(truncate(error.getMessage(), 900));
        if (event.getRetryCount() >= maxRetries) {
            event.setStatus(OutboxEvent.Status.FAILED);
            log.error("Outbox event {} FAILED after {} retries: {}",
                    event.getId(), event.getRetryCount(), error.getMessage());
            activityLogService.log(null, ActivityLogService.OUTBOX_FAILED, "OutboxEvent",
                    event.getId(), event.getEventType() + ": " + truncate(error.getMessage(), 200));
        } else {
            log.warn("Outbox event {} retry {}/{}: {}",
                    event.getId(), event.getRetryCount(), maxRetries, error.getMessage());
        }
        outboxEventRepository.save(event);
    }

    private void dispatch(OutboxEvent event) throws Exception {
        JsonNode payload = objectMapper.readTree(event.getPayload());
        String type = event.getEventType();

        if (OutboxEvent.TYPE_CONTRACT_GENERATE.equals(type)) {
            UUID bookingId = UUID.fromString(payload.get("bookingId").asText());
            User actor = resolveActor(payload);
            contractService.processContractGenerateFromOutbox(bookingId, actor);
            return;
        }

        if (OutboxEvent.TYPE_CONTRACT_EMAIL_SEND.equals(type)) {
            UUID contractId = UUID.fromString(payload.get("contractId").asText());
            String targetEmail = payload.hasNonNull("targetEmail") ? payload.get("targetEmail").asText() : null;
            User actor = resolveActor(payload);
            contractService.processContractEmailSendFromOutbox(contractId, targetEmail, actor);
            return;
        }

        if (OutboxEvent.TYPE_CONTRACT_RESEND.equals(type)) {
            UUID contractId = UUID.fromString(payload.get("contractId").asText());
            String targetEmail = payload.hasNonNull("targetEmail") ? payload.get("targetEmail").asText() : null;
            User actor = resolveActor(payload);
            contractService.processContractResendFromOutbox(contractId, targetEmail, actor);
            return;
        }

        if (NOTIFICATION_EVENT_TYPES.contains(type)) {
            notificationDispatchHandler.handle(event, payload);
            return;
        }

        throw new IllegalArgumentException("Unknown outbox event type: " + type);
    }

    private User resolveActor(JsonNode payload) {
        JsonNode actorNode = payload.get("actorUserId");
        if (actorNode == null || actorNode.asText().isBlank()) {
            return null;
        }
        try {
            return userRepository.findById(UUID.fromString(actorNode.asText())).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String truncate(String msg, int max) {
        if (msg == null) return null;
        return msg.length() <= max ? msg : msg.substring(0, max);
    }
}
