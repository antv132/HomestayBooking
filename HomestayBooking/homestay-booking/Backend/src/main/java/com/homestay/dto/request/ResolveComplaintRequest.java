package com.homestay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** SCR-54: body cho Admin resolve complaint. */
@Data
public class ResolveComplaintRequest {

    @NotBlank(message = "Ghi chú giải quyết là bắt buộc")
    @Size(max = 2000, message = "Ghi chú giải quyết tối đa 2000 ký tự")
    private String resolution;
}
