package com.homestay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * FR-09 SCR-51 — PATCH customer status (ACTIVE | SUSPENDED only).
 */
@Data
public class UpdateCustomerStatusRequest {

    @NotBlank
    private String status;
}
