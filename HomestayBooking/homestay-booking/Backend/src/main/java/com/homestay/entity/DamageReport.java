package com.homestay.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "damage_reports",
    indexes = {
        @Index(name = "idx_dr_inspection", columnList = "inspection_id", unique = true),
        @Index(name = "idx_dr_booking",    columnList = "booking_id"),
        @Index(name = "idx_dr_status",     columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class DamageReport {

    public enum Status { DRAFT, PENDING_APPROVAL, APPROVED, DISPUTED, PAID }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false, unique = true)
    private RoomInspection inspection;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "total_estimated_cost", precision = 15, scale = 2)
    private BigDecimal totalEstimatedCost;

    // Số tiền Manager phê duyệt
    @Column(name = "approved_amount", precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // Tự động set true nếu totalEstimatedCost > 5,000,000 VND
    @Column(name = "requires_admin_escalation", nullable = false)
    private Boolean requiresAdminEscalation = false;

    // Admin đồng phê duyệt (bắt buộc nếu > 5M)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_approver_id")
    private User adminApprover;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Status status = Status.DRAFT;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    /** Customer dispute note (023 FR-013). */
    @Column(name = "dispute_note", columnDefinition = "TEXT")
    private String disputeNote;

    @Column(name = "disputed_at")
    private LocalDateTime disputedAt;

    @OneToMany(mappedBy = "damageReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DamageItem> items;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
