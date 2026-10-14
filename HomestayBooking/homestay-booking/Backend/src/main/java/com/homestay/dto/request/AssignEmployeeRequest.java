package com.homestay.dto.request;



import jakarta.validation.constraints.NotNull;

import lombok.Data;



import java.util.UUID;



@Data

public class AssignEmployeeRequest {



    @NotNull(message = "Cần chọn nhân viên")

    private UUID employeeId;



    @NotNull(message = "Cần chọn homestay")

    private UUID propertyId;

}

