package com.homestay.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateRoomRequest {

    // Optional fields — all null-safe in RoomService.update()
    // Validation only triggers when value is provided (non-null)

    private String floorId;

    private String roomNumber;   // no @NotBlank — optional partial update

    private String roomType;     // Studio / Standard / Deluxe / Suite / Villa

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal pricePerNight;

    @Min(value = 1, message = "Sức chứa tối thiểu 1 người")
    private Integer capacity;

    @DecimalMin(value = "0.0", message = "Diện tích phải >= 0")
    private BigDecimal area;

    private String description;

    private List<String> amenities;
}
