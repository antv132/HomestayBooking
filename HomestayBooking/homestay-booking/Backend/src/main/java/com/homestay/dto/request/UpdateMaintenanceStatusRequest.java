package com.homestay.dto.request;

import com.homestay.entity.MaintenanceTicket;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMaintenanceStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private MaintenanceTicket.Status status;

    /** Bắt buộc khi chuyển sang RESOLVED (ghi chú vật tư / cách xử lý). */
    @Size(max = 1000, message = "Ghi chú xử lý tối đa 1000 ký tự")
    private String resolutionNote;
}
