package com.homestay.dto.response;

import com.homestay.entity.Promotion;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class PromotionResponse {
    private UUID id;
    private String subtitle;
    private String title;
    private String description;
    private String ctaText;
    private String ctaUrl;
    private String imageUrl;
    private String colorTheme;
    private boolean isActive;
    private int sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PromotionResponse fromEntity(Promotion p) {
        PromotionResponse r = new PromotionResponse();
        r.setId(p.getId());
        r.setSubtitle(p.getSubtitle());
        r.setTitle(p.getTitle());
        r.setDescription(p.getDescription());
        r.setCtaText(p.getCtaText());
        r.setCtaUrl(p.getCtaUrl());
        r.setImageUrl(p.getImageUrl());
        r.setColorTheme(p.getColorTheme());
        r.setActive(p.isActive());
        r.setSortOrder(p.getSortOrder());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }
}
