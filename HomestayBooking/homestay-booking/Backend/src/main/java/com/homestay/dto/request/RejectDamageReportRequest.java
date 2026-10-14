package com.homestay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** SCR-43: Manager từ chối báo cáo hư hại (trả về Employee sửa lại). */
@Data
public class RejectDamageReportRequest {

    @NotBlank(message = "Cần nhập lý do từ chối")
    private String reason;
}
