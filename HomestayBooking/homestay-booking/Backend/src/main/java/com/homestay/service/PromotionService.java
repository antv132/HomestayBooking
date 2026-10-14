package com.homestay.service;

import com.homestay.dto.response.PromotionResponse;
import com.homestay.repository.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/** Public / landing promotion reads (FR-18 US1). Admin CRUD lives in AdminPromotionService. */
@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    /** Public: Active only — sortOrder ASC, createdAt DESC. */
    @Transactional(readOnly = true)
    public List<PromotionResponse> getActivePromotions() {
        return promotionRepository.findByIsActiveTrueOrderBySortOrderAscCreatedAtDesc()
                .stream()
                .map(PromotionResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
