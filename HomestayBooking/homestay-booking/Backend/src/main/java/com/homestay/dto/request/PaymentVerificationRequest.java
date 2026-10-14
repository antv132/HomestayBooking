package com.homestay.dto.request;

import com.homestay.entity.Payment;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentVerificationRequest {
    
    @NotNull(message = "Status cannot be null")
    private Payment.Status status; // PAID or FAILED
    
    private String note;
}
