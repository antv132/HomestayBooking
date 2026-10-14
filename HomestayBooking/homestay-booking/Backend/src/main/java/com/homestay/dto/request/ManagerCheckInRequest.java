package com.homestay.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ManagerCheckInRequest {

    @NotEmpty(message = "Cần ít nhất một ảnh giấy tờ")
    @Size(min = 1, max = 3, message = "Tối đa 3 ảnh giấy tờ")
    private List<String> idDocumentUrls;

    @NotNull(message = "Phải xác nhận đã giao chìa khóa")
    private Boolean keyHandedOver;

    private Boolean remainingCollected;

    @Size(max = 500)
    private String note;
}
