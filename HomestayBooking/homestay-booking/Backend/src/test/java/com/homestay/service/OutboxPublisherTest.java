package com.homestay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay.entity.OutboxEvent;
import com.homestay.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock OutboxEventRepository outboxEventRepository;

    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutboxPublisher(outboxEventRepository, new ObjectMapper());
    }

    @Test
    void enqueueContractGenerate_persistsPendingEvent() {
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> {
            OutboxEvent e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        UUID bookingId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        publisher.enqueueContractGenerate(bookingId, actorId);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent saved = captor.getValue();
        assertEquals(OutboxEvent.TYPE_CONTRACT_GENERATE, saved.getEventType());
        assertEquals(OutboxEvent.Status.PENDING, saved.getStatus());
        assertTrue(saved.getPayload().contains(bookingId.toString()));
        assertTrue(saved.getPayload().contains(actorId.toString()));
    }

    @Test
    void enqueueContractResend_persistsPendingEvent() {
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> {
            OutboxEvent e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        UUID contractId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        publisher.enqueueContractResend(contractId, "guest@example.com", actorId);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent saved = captor.getValue();
        assertEquals(OutboxEvent.TYPE_CONTRACT_RESEND, saved.getEventType());
        assertEquals(OutboxEvent.Status.PENDING, saved.getStatus());
        assertTrue(saved.getPayload().contains(contractId.toString()));
        assertTrue(saved.getPayload().contains("guest@example.com"));
        assertTrue(saved.getPayload().contains(actorId.toString()));
    }
}
