package com.homestay.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AssignHousekeepingTaskRequest {

    @NotNull(message = "Cần chọn nhân viên")
    private UUID assigneeId;
}
