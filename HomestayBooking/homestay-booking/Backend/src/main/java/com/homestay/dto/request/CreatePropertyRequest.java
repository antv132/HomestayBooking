package com.homestay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePropertyRequest {

    @NotBlank(message = "Tên property không được để trống")
    @Size(max = 200, message = "Tên property tối đa 200 ký tự")
    private String name;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    private String address;

    private String description;

    // ACTIVE hoặc INACTIVE (mặc định ACTIVE nếu không truyền)
    private String status;
}
