package com.homestay.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO cho SCR-50 — Admin sửa tài khoản Manager (Modal Edit Manager).
 * password tùy chọn: để trống thì giữ mật khẩu cũ.
 * status tùy chọn: ACTIVE | INACTIVE.
 */
@Data
public class AdminUpdateManagerRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email tối đa 255 ký tự")
    private String email;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    /** Để trống / null = không đổi mật khẩu. Validate độ dài ở service khi có giá trị. */
    private String password;

    /** ACTIVE | INACTIVE — tùy chọn. */
    private String status;
}
