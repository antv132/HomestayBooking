package com.homestay.dto.request;



import jakarta.validation.constraints.Size;

import lombok.Data;



@Data

public class CancelHousekeepingTaskRequest {



    @Size(max = 1000)

    private String note;

}

