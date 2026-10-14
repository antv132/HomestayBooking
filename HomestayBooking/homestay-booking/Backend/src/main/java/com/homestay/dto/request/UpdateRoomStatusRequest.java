package com.homestay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateRoomStatusRequest {

    @NotBlank(message = "Trạng thái không được để trống")
    private String status;

    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 1000, message = "Lý do tối đa 1000 ký tự")
    private String reason;

    /** Legacy alias — mapped to reason in service when reason is null */
    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;
}
