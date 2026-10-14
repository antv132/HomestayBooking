package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancellationPreviewResponse {
    private int daysUntilCheckIn;
    private int refundPercent;
    private BigDecimal refundAmount;
    private BigDecimal forfeitAmount;
    private String policyText;
}
