package com.homestay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 023 FR-013 — Customer dispute within 24h of approval. */
@Data
public class DisputeDamageReportRequest {

    @NotBlank(message = "Ghi chú tranh chấp không được để trống")
    @Size(min = 10, max = 2000, message = "Ghi chú tranh chấp phải từ 10 đến 2000 ký tự")
    private String note;
}
