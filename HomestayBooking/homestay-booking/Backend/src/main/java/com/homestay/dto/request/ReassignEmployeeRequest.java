package com.homestay.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ReassignEmployeeRequest {

    @NotNull(message = "Cần chọn homestay mới")
    private UUID propertyId;
}
