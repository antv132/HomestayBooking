package com.homestay.service;

import com.homestay.dto.response.PageResponse;
import com.homestay.entity.OutboxEvent;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.OutboxEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** FR-15 US6 — Admin monitor/retry notification outbox failures. */
@Service
public class OutboxAdminService {

    public static final List<String> NOTIFICATION_EVENT_TYPES = List.of(
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
    private final ActivityLogService activityLogService;

    public OutboxAdminService(OutboxEventRepository outboxEventRepository,
                              ActivityLogService activityLogService) {
        this.outboxEventRepository = outboxEventRepository;
        this.activityLogService = activityLogService;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> list(OutboxEvent.Status status, Pageable pageable) {
        Page<OutboxEvent> page = outboxEventRepository.findByStatusAndEventTypeIn(
                status, NOTIFICATION_EVENT_TYPES, pageable);
        List<Map<String, Object>> content = page.getContent().stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        return new PageResponse<>(
                content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional
    public Map<String, Object> retry(UUID outboxId, User admin) {
        OutboxEvent event = outboxEventRepository.findById(outboxId)
                .orElseThrow(() -> new ResourceNotFoundException("Outbox event not found"));
        if (!NOTIFICATION_EVENT_TYPES.contains(event.getEventType())) {
            throw new BusinessException("Chỉ retry được event notification");
        }
        if (event.getStatus() != OutboxEvent.Status.FAILED) {
            throw new BusinessException("Chỉ retry event ở trạng thái FAILED");
        }
        event.setStatus(OutboxEvent.Status.PENDING);
        event.setLastError(null);
        outboxEventRepository.save(event);
        activityLogService.log(admin, ActivityLogService.OUTBOX_RETRY_REQUESTED, "OutboxEvent",
                event.getId(), event.getEventType());
        return toMap(event);
    }

    private Map<String, Object> toMap(OutboxEvent e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId().toString());
        m.put("eventType", e.getEventType());
        m.put("status", e.getStatus().name());
        m.put("retryCount", e.getRetryCount());
        m.put("errorSummary", e.getLastError());
        m.put("createdAt", e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        m.put("processedAt", e.getProcessedAt() != null ? e.getProcessedAt().toString() : null);
        return m;
    }
}
