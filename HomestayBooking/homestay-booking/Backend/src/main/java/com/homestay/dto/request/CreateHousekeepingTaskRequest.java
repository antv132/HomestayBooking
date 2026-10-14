package com.homestay.dto.request;



import jakarta.validation.constraints.NotNull;

import lombok.Data;



import java.util.UUID;



@Data

public class CreateHousekeepingTaskRequest {



    @NotNull(message = "Cần chọn phòng")

    private UUID roomId;



    private UUID assigneeId;

}

