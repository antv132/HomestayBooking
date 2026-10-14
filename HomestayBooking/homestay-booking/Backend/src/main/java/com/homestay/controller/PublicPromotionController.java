package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.PromotionResponse;
import com.homestay.service.PromotionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** FR-18 US1 — Guest public active promotions (SCR-01). */
@RestController
@RequestMapping("/api/v1/public/promotions")
public class PublicPromotionController {

    private final PromotionService promotionService;

    public PublicPromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> listActive() {
        return ResponseEntity.ok(ApiResponse.ok(promotionService.getActivePromotions()));
    }
}
