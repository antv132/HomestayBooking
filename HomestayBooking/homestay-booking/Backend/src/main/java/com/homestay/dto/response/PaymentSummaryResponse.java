package com.homestay.dto.response;

import com.homestay.entity.Payment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentSummaryResponse {
    private UUID id;
    private UUID bookingId;
    private String customerName;
    private Payment.Type type;
    private Payment.Method method;
    private BigDecimal amount;
    private Payment.Status status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    public static PaymentSummaryResponse fromEntity(Payment payment) {
        return PaymentSummaryResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBooking().getId())
                .customerName(payment.getCustomer().getFullName())
                .type(payment.getType())
                .method(payment.getMethod())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
