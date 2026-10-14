package com.homestay.job;

import com.homestay.service.OutboxProcessorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorJobTest {

    @Mock OutboxProcessorService outboxProcessorService;

    private OutboxProcessorJob job;

    @BeforeEach
    void setUp() {
        job = new OutboxProcessorJob(outboxProcessorService);
    }

    @Test
    void poll_callsService() {
        when(outboxProcessorService.processPendingBatch()).thenReturn(5);

        job.poll();

        verify(outboxProcessorService).processPendingBatch();
    }

    @Test
    void poll_noEvents_logsNothing() {
        when(outboxProcessorService.processPendingBatch()).thenReturn(0);

        job.poll();

        verify(outboxProcessorService).processPendingBatch();
    }

    @Test
    void poll_someEvents_logsCount() {
        when(outboxProcessorService.processPendingBatch()).thenReturn(12);

        job.poll();

        verify(outboxProcessorService).processPendingBatch();
    }
}
