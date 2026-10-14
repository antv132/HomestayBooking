package com.homestay.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO cho SCR-48 — Admin cập nhật Property (partial update).
 * Field null = không đổi; description blank string → clear.
 */
@Data
public class AdminUpdatePropertyRequest {

    @Size(max = 200, message = "Tên property tối đa 200 ký tự")
    private String name;

    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    private String address;

    @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự")
    private String description;

    /** ACTIVE | INACTIVE — parse ở service, giá trị không hợp lệ sẽ bị bỏ qua. */
    private String status;
}
