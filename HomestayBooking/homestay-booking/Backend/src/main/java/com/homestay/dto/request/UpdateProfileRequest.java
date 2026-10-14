package com.homestay.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 255, message = "Họ tên tối đa 255 ký tự")
    private String fullName;

    private String phone;

    @Size(max = 512, message = "URL avatar tối đa 512 ký tự")
    private String avatarUrl;
}