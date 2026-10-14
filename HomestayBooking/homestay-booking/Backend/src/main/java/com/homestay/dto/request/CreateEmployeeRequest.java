package com.homestay.dto.request;



import jakarta.validation.constraints.Email;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Pattern;

import jakarta.validation.constraints.Size;

import lombok.Data;



import java.util.UUID;



@Data

public class CreateEmployeeRequest {



    @NotBlank(message = "Họ tên không được để trống")

    @Size(max = 200, message = "Họ tên không được vượt quá 200 ký tự")

    private String fullName;



    @NotBlank(message = "Email không được để trống")

    @Email(message = "Email không hợp lệ")

    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")

    private String email;



    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 chữ số")

    @Pattern(regexp = "^\\d*$", message = "Số điện thoại chỉ được chứa chữ số")

    private String phone;



    @NotNull(message = "Cần chọn homestay")

    private UUID propertyId;

}

