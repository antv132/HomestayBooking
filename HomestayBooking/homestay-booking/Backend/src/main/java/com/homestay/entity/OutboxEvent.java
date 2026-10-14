package com.homestay.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transactional outbox for async side-effects (contract generate/email).
 * FR-10 / TD-006.
 */
@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_status_created", columnList = "status, created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    public enum Status { PENDING, PROCESSED, FAILED }

    public static final String TYPE_CONTRACT_GENERATE = "CONTRACT_GENERATE";
    public static final String TYPE_CONTRACT_EMAIL_SEND = "CONTRACT_EMAIL_SEND";
    public static final String TYPE_CONTRACT_RESEND = "CONTRACT_RESEND";

    /** FR-15 — generic notification dispatch payload. */
    public static final String TYPE_NOTIFICATION_DISPATCH = "NOTIFICATION_DISPATCH";
    public static final String TYPE_BOOKING_CONFIRMED = "BOOKING_CONFIRMED";
    public static final String TYPE_CONTRACT_GENERATED = "CONTRACT_GENERATED";
    public static final String TYPE_PAYMENT_PENDING_VERIFICATION = "PAYMENT_PENDING_VERIFICATION";
    public static final String TYPE_PAYMENT_CONFIRMED = "PAYMENT_CONFIRMED";
    public static final String TYPE_MAINTENANCE_STATUS_CHANGED = "MAINTENANCE_STATUS_CHANGED";
    public static final String TYPE_AVAILABILITY_UPDATED = "AVAILABILITY_UPDATED";
    public static final String TYPE_SYSTEM_BROADCAST = "SYSTEM_BROADCAST";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
