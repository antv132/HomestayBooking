package com.homestay.dto.response;

import com.homestay.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO response cho SCR-52 - Payment Reconciliation (Admin).
 * vnpayStatus/systemStatus/discrepancyReason la suy dien tu Payment (entity khong luu).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReconciliationResponse {

    private UUID id;
    private UUID bookingId;
    private BigDecimal amount;
    private String vnpayStatus;
    private String systemStatus;
    private String discrepancyReason;
    private LocalDateTime createdAt;

    public static PaymentReconciliationResponse fromEntity(Payment p) {
        String code = p.getGatewayResponseCode();
        String vnpayStatus;
        if ("00".equals(code)) {
            vnpayStatus = "SUCCESS";
        } else if (code == null) {
            vnpayStatus = "NO_RESPONSE";
        } else {
            vnpayStatus = "FAILED_" + code;
        }

        boolean gatewaySuccess = "00".equals(code);
        boolean paid = p.getStatus() == Payment.Status.PAID;
        String reason;
        if (gatewaySuccess && !paid) {
            reason = "Gateway success but system not PAID";
        } else if (paid && !gatewaySuccess) {
            reason = "System PAID but gateway not confirmed";
        } else {
            reason = "Pending without IPN (timeout)";
        }

        return PaymentReconciliationResponse.builder()
                .id(p.getId())
                .bookingId(p.getBooking().getId())
                .amount(p.getAmount())
                .vnpayStatus(vnpayStatus)
                .systemStatus(p.getStatus().name())
                .discrepancyReason(reason)
                .createdAt(p.getCreatedAt())
                .build();
    }
}