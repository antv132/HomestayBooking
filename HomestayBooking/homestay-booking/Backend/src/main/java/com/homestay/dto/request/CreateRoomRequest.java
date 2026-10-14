package com.homestay.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateRoomRequest {

    @NotNull(message = "Property ID không được để trống")
    private String propertyId;

    @NotNull(message = "Floor ID không được để trống")
    private String floorId;

    @NotBlank(message = "Số phòng không được để trống")
    private String roomNumber;

    @NotBlank(message = "Loại phòng không được để trống")
    private String roomType; // Studio / Standard / Deluxe / Suite / Villa

    @NotNull(message = "Giá theo đêm không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal pricePerNight;

    @NotNull(message = "Sức chứa không được để trống")
    @Min(value = 1, message = "Sức chứa tối thiểu 1 người")
    private Integer capacity;

    @DecimalMin(value = "0.0", message = "Diện tích phải >= 0")
    private BigDecimal area;

    private String description;

    private List<String> amenities;
}
