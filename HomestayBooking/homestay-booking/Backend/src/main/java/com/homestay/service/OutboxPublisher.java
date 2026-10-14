package com.homestay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay.entity.OutboxEvent;
import com.homestay.exception.BusinessException;
import com.homestay.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/** Enqueue outbox events in the same transaction as the domain write. */
@Service
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OutboxEvent enqueue(String eventType, Map<String, Object> payload) {
        OutboxEvent event = new OutboxEvent();
        event.setEventType(eventType);
        event.setStatus(OutboxEvent.Status.PENDING);
        try {
            event.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new BusinessException("Không serialize được outbox payload");
        }
        OutboxEvent saved = outboxEventRepository.save(event);
        log.info("Outbox enqueued type={} id={}", eventType, saved.getId());
        return saved;
    }

    @Transactional
    public void enqueueContractGenerate(UUID bookingId, UUID actorUserId) {
        enqueue(OutboxEvent.TYPE_CONTRACT_GENERATE, Map.of(
                "bookingId", bookingId.toString(),
                "actorUserId", actorUserId != null ? actorUserId.toString() : ""
        ));
    }

    /** FR-10 — after PDF persisted, queue email send as separate outbox step. */
    @Transactional
    public void enqueueContractEmailSend(UUID contractId, UUID actorUserId) {
        enqueue(OutboxEvent.TYPE_CONTRACT_EMAIL_SEND, Map.of(
                "contractId", contractId.toString(),
                "actorUserId", actorUserId != null ? actorUserId.toString() : ""
        ));
    }

    /** FR-009 — Manager resend: queue only; worker sends without regenerating PDF. */
    @Transactional
    public void enqueueContractResend(UUID contractId, String targetEmail, UUID actorUserId) {
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("contractId", contractId.toString());
        payload.put("actorUserId", actorUserId != null ? actorUserId.toString() : "");
        if (targetEmail != null && !targetEmail.isBlank()) {
            payload.put("targetEmail", targetEmail.trim());
        }
        enqueue(OutboxEvent.TYPE_CONTRACT_RESEND, payload);
    }
}
