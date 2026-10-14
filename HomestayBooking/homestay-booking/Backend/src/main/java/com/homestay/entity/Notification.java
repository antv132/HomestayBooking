package com.homestay.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications",
    indexes = {
        @Index(name = "idx_notif_user",    columnList = "user_id, is_read"),
        @Index(name = "idx_notif_created", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    // Các loại thông báo hệ thống gửi tự động (FR-15 + legacy)
    public enum Type {
        BOOKING_CONFIRMED,
        BOOKING_CANCELLED,
        CONTRACT_GENERATED,
        PAYMENT_CONFIRMED,
        PAYMENT_PENDING_VERIFICATION,
        MAINTENANCE_UPDATED,
        HOUSEKEEPING_TASK_UPDATED,
        AVAILABILITY_UPDATED,
        SYSTEM
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private Type type;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    // false = chưa đọc (hiện chấm cam), true = đã đọc
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    // UUID của entity liên quan (để điều hướng khi click)
    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    // Tên class entity liên quan: "Booking", "Contract", "Payment"
    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    /** Idempotency key — FR-15 FR-012 */
    @Column(name = "dedupe_key", length = 128)
    private String dedupeKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
