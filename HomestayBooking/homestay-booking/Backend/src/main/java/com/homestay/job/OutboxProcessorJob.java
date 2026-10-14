package com.homestay.job;

import com.homestay.service.OutboxProcessorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Polls PENDING outbox events (contract generate/email). FR-10 / TD-006. */
@Component
public class OutboxProcessorJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessorJob.class);

    private final OutboxProcessorService outboxProcessorService;

    public OutboxProcessorJob(OutboxProcessorService outboxProcessorService) {
        this.outboxProcessorService = outboxProcessorService;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-ms:15000}")
    public void poll() {
        int n = outboxProcessorService.processPendingBatch();
        if (n > 0) {
            log.info("Outbox processed {} event(s)", n);
        }
    }
}
