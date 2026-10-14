package com.homestay.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Request DTO cho SCR-49 — Admin gán Manager vào Property.
 */
@Data
public class AssignManagerRequest {

    @NotNull(message = "managerId bắt buộc")
    private UUID managerId;
}
