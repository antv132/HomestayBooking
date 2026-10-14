package com.homestay.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** SCR-20 — body for POST /api/v1/payments/vnpay (deposit via VNPay). */
@Getter
@Setter
public class CreateVnpayPaymentRequest {

    @NotNull(message = "bookingId is required")
    private UUID bookingId;
}
