package com.homestay.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_payments_booking",  columnList = "booking_id"),
        @Index(name = "idx_payments_customer", columnList = "customer_id"),
        @Index(name = "idx_payments_status",   columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    // Loại thanh toán: đặt cọc 40%, thanh toán phần còn lại 60%, hoặc phí bồi thường
    public enum Type { DEPOSIT, REMAINING_BALANCE, DAMAGE_FEE }

    // Phương thức thanh toán
    public enum Method { BANK_TRANSFER, CASH, VNPAY }

    // PENDING / PAID / FAILED / REFUNDED (100% hoàn) / PARTIALLY_REFUNDED (FR-010 50% tier; amount giữ nguyên số cọc đã thu)
    public enum Status { PENDING, PAID, FAILED, REFUNDED, PARTIALLY_REFUNDED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // Lưu thêm customer để query nhanh không cần join qua booking
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private Type type;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private Method method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.PENDING;

    // --- VNPay Gateway fields ---
    // Mã giao dịch cổng thanh toán
    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId;

    // Mã đơn hàng đối soát (Idempotency Key)
    @Column(name = "order_ref", length = 100)
    private String orderRef;

    // Mã lỗi từ VNPay
    @Column(name = "gateway_response_code", length = 10)
    private String gatewayResponseCode;

    // Thời gian nhận callback IPN
    @Column(name = "ipn_received_at")
    private LocalDateTime ipnReceivedAt;

    // Manager nào đã duyệt
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // Ghi chú khi duyệt/từ chối
    @Column(name = "verification_note", columnDefinition = "TEXT")
    private String verificationNote;

    /** FR-11 link — set when paying against an invoice. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
