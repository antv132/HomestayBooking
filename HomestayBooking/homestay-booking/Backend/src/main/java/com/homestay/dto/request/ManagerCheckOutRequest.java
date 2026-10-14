package com.homestay.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ManagerCheckOutRequest {

    @NotNull(message = "Phải xác nhận đã thu lại chìa khóa")
    private Boolean keyReturned;

    @Size(max = 500)
    private String note;
}
