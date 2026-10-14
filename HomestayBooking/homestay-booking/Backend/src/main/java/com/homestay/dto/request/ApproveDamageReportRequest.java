package com.homestay.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

/** SCR-43: Manager duyệt báo cáo hư hại. approvedAmount optional (mặc định = totalEstimatedCost). */
@Data
public class ApproveDamageReportRequest {

    @DecimalMin(value = "0.0", inclusive = false, message = "Số tiền duyệt phải lớn hơn 0")
    private BigDecimal approvedAmount;

    private String note;
}
