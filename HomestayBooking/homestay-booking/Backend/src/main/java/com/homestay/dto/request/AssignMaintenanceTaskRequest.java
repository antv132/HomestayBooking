package com.homestay.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/** SCR-41: Manager gán kỹ thuật viên cho yêu cầu bảo trì. */
@Data
public class AssignMaintenanceTaskRequest {

    @NotNull(message = "Cần chọn kỹ thuật viên")
    private UUID assigneeId;
}
