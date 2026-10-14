package com.homestay.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ReorderImagesRequest {

    @NotEmpty(message = "Danh sách ảnh không được để trống")
    private List<String> imageIds;
}
