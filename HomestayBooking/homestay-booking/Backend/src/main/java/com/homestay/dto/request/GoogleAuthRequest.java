package com.homestay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// Frontend gửi Google ID Token lên sau khi user đăng nhập Google thành công
// Backend verify token với Google và tạo/tìm user tương ứng
@Data
public class GoogleAuthRequest {

    @NotBlank(message = "Google ID Token không được để trống")
    private String idToken;
}
