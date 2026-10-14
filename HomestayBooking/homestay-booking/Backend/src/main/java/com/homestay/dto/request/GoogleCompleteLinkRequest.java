package com.homestay.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GoogleCompleteLinkRequest {

    @NotBlank(message = "verificationToken không được để trống")
    private String verificationToken;

    @NotBlank(message = "Mã OTP không được để trống")
    @Size(min = 6, max = 6, message = "Mã OTP phải đúng 6 ký tự")
    @JsonAlias("otp")
    private String otpCode;
}
