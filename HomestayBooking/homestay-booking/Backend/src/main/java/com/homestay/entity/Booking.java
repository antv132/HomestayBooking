package com.homestay.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "bookings",
    indexes = {
        @Index(name = "idx_bookings_customer",  columnList = "customer_id"),
        @Index(name = "idx_bookings_room",       columnList = "room_id"),
        @Index(name = "idx_bookings_status",     columnList = "status"),
        @Index(name = "idx_bookings_check_in",   columnList = "check_in_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    /*
     * Vòng đời booking:
     * PENDING_DEPOSIT -> (khách cọc, manager duyệt) -> CONFIRMED
     * CONFIRMED -> (check-in) -> CHECKED_IN
     * CHECKED_IN -> (inspection) -> PENDING_INSPECTION
     * PENDING_INSPECTION -> (nếu hư hại) -> PENDING_DAMAGE_PAYMENT
     * PENDING_INSPECTION / PENDING_DAMAGE_PAYMENT -> (check-out) -> CHECKED_OUT
     * Bất kỳ lúc nào trước check-in -> CANCELLED (tiền cọc không hoàn)
     * Quá hạn check-in -> NO_SHOW
     */
    public enum Status {
        PENDING_DEPOSIT,
        CONFIRMED,
        CHECKED_IN,
        PENDING_INSPECTION,
        PENDING_DAMAGE_PAYMENT,
        CHECKED_OUT,
        CANCELLED,
        NO_SHOW
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "guest_count", nullable = false)
    private Integer guestCount;

    // Tổng tiền = pricePerNight * số đêm
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    // 40% tổng tiền (lấy từ SystemSetting.DEPOSIT_PERCENTAGE)
    @Column(name = "deposit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal depositAmount;

    // 60% còn lại
    @Column(name = "remaining_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingAmount;

    // Phí bồi thường hư hại (được cộng sau khi Manager approve DamageReport)
    @Column(name = "damage_fee_amount", precision = 15, scale = 2)
    private BigDecimal damageFeeAmount;

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Status status = Status.PENDING_DEPOSIT;

    // Thời hạn giữ phòng (timeout mặc định 10 phút chờ thanh toán cọc)
    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    // Optimistic locking cho việc tránh overbooking
    @Version
    @Column(name = "row_version", nullable = false, columnDefinition = "int default 0")
    private Integer rowVersion = 0;

    // Thông tin hủy booking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments;

    // Contract được tạo tự động sau khi duyệt tiền cọc
    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private Contract contract;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
