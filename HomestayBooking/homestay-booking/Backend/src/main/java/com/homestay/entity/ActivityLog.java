package com.homestay.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "activity_logs",
    indexes = {
        @Index(name = "idx_actlog_user",   columnList = "user_id"),
        @Index(name = "idx_actlog_action", columnList = "action"),
        @Index(name = "idx_actlog_created",columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ActivityLog {

    /*
     * Các action được log:
     * USER_LOGIN, USER_LOGOUT
     * BOOKING_CREATED, BOOKING_CANCELLED
     * PAYMENT_CONFIRMED, PAYMENT_REJECTED
     * CONTRACT_GENERATED, CONTRACT_RESENT
     * ROOM_STATUS_CHANGED
     * USER_SUSPENDED, USER_ACTIVATED
     * SYSTEM_SETTINGS_CHANGED
     */

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // null nếu là hành động của hệ thống (auto-generate)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @BatchSize(size = 25)
    private User user;

    @Column(name = "action", nullable = false, length = 60)
    private String action;

    // Tên class entity bị ảnh hưởng: "Booking", "Payment", "Room"
    @Column(name = "entity_type", length = 60)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
