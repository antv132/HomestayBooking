package com.homestay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO cho SCR-47 — Admin tạo Property.
 * FE gửi name + address (+ optional description, status).
 */
@Data
public class AdminCreatePropertyRequest {

    @NotBlank(message = "Tên property không được để trống")
    @Size(max = 200, message = "Tên property tối đa 200 ký tự")
    private String name;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    private String address;

    @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự")
    private String description;

    /** ACTIVE | INACTIVE — mặc định INACTIVE nếu bỏ trống (FR-06). */
    private String status;
}
