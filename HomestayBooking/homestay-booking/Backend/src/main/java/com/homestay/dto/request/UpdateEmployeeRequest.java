package com.homestay.dto.request;



import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;

import lombok.Data;



@Data

public class UpdateEmployeeRequest {



    @NotBlank(message = "Họ tên không được để trống")

    @Size(max = 200)

    private String fullName;



    @Size(max = 20)

    private String phone;

}

