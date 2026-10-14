package com.homestay.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** SCR-53: body cho Admin co-approve damage report. */
@Data
public class CoApproveDamageRequest {

    @NotNull(message = "approvedFee bat buoc")
    @DecimalMin(value = "0", inclusive = false, message = "approvedFee phai lon hon 0")
    private BigDecimal approvedFee;
}