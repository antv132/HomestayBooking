package com.homestay.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatchPromotionActiveRequest {

    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
