package com.homestay.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PromotionRequest {

    @NotBlank(message = "Subtitle is required")
    @Size(min = 2, max = 100, message = "Subtitle phải từ 2–100 ký tự")
    private String subtitle;

    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 200, message = "Title phải từ 2–200 ký tự")
    private String title;

    @Size(max = 400, message = "Description tối đa 400 ký tự")
    private String description;

    @NotBlank(message = "CTA text is required")
    @Size(min = 2, max = 80, message = "CTA text phải từ 2–80 ký tự")
    private String ctaText;

    @NotBlank(message = "CTA URL is required")
    @Size(max = 300, message = "CTA URL tối đa 300 ký tự")
    private String ctaUrl;

    /** URL ảnh banner (tùy chọn) */
    @Size(max = 500, message = "imageUrl tối đa 500 ký tự")
    private String imageUrl;

    @NotBlank(message = "Color theme is required")
    private String colorTheme;

    @NotNull
    private Boolean isActive = true;

    @Min(value = 0, message = "sortOrder phải ≥ 0")
    @Max(value = 9999, message = "sortOrder quá lớn")
    private int sortOrder = 0;
}
