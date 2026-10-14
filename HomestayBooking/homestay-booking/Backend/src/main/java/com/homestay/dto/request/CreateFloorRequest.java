package com.homestay.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateFloorRequest {

    @NotNull(message = "Property ID không được để trống")
    private String propertyId;

    @NotNull(message = "Số tầng không được để trống")
    @Min(value = 1, message = "Số tầng phải >= 1")
    private Integer floorNumber;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;
}
