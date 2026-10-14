package com.homestay.dto.response;

import com.homestay.entity.Payment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentDetailResponse {
    private UUID id;
    private UUID bookingId;
    private UUID customerId;
    private String customerName;
    private Payment.Type type;
    private Payment.Method method;
    private BigDecimal amount;
    private Payment.Status status;

    private String orderRef;
    private String gatewayTransactionId;
    private String gatewayResponseCode;

    private String verifiedByName;
    private LocalDateTime verifiedAt;
    private LocalDateTime paidAt;
    private String verificationNote;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentDetailResponse fromEntity(Payment payment) {
        String verifiedByName = payment.getVerifiedBy() != null ? payment.getVerifiedBy().getFullName() : null;

        return PaymentDetailResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBooking().getId())
                .customerId(payment.getCustomer().getId())
                .customerName(payment.getCustomer().getFullName())
                .type(payment.getType())
                .method(payment.getMethod())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .orderRef(payment.getOrderRef())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .gatewayResponseCode(payment.getGatewayResponseCode())
                .verifiedByName(verifiedByName)
                .verifiedAt(payment.getVerifiedAt())
                .paidAt(payment.getPaidAt())
                .verificationNote(payment.getVerificationNote())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
