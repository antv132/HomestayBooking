package com.homestay.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/** SCR-42 — Manager gán / đổi Employee kiểm tra phòng. */
@Data
public class AssignInspectorRequest {

    @NotNull(message = "Cần chọn nhân viên kiểm tra")
    private UUID employeeId;
}
