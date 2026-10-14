package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.PromotionResponse;
import com.homestay.service.PromotionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Legacy public promotion endpoints kept for compatibility.
 * Canonical FR-18 path: {@code GET /api/v1/public/promotions}.
 */
@RestController
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping("/api/public/promotions")
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> getActiveLegacy() {
        return ResponseEntity.ok(ApiResponse.ok(promotionService.getActivePromotions()));
    }
}
