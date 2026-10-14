package com.homestay.dto.request;



import com.homestay.entity.User;

import jakarta.validation.constraints.NotNull;

import lombok.Data;



@Data

public class UpdateEmployeeStatusRequest {



    @NotNull(message = "Trạng thái không được để trống")

    private User.Status status;

}

