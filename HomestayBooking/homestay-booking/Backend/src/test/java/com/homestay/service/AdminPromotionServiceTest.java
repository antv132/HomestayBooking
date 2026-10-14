package com.homestay.service;

import com.homestay.dto.request.PromotionRequest;
import com.homestay.dto.response.PromotionResponse;
import com.homestay.entity.Promotion;
import com.homestay.exception.BusinessException;
import com.homestay.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPromotionServiceTest {

    @Mock PromotionRepository promotionRepository;

    private AdminPromotionService adminService;
    private PromotionService publicService;

    @BeforeEach
    void setUp() {
        adminService = new AdminPromotionService(promotionRepository);
        publicService = new PromotionService(promotionRepository);
    }

    @Test
    void create_rejectsJavascriptCtaUrl() {
        PromotionRequest req = validRequest();
        req.setCtaUrl("javascript:alert(1)");

        BusinessException ex = assertThrows(BusinessException.class, () -> adminService.create(req));
        assertTrue(ex.getMessage().toLowerCase().contains("ctaurl")
                || ex.getMessage().toLowerCase().contains("scheme"));
    }

    @Test
    void create_rejectsInvalidColorTheme() {
        PromotionRequest req = validRequest();
        req.setColorTheme("neon");

        BusinessException ex = assertThrows(BusinessException.class, () -> adminService.create(req));
        assertTrue(ex.getMessage().toLowerCase().contains("colortheme"));
    }

    @Test
    void create_normalizesRelativeCtaAndTheme() {
        PromotionRequest req = validRequest();
        req.setCtaUrl("/search");
        req.setColorTheme("Blue");
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(inv -> {
            Promotion p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            return p;
        });

        PromotionResponse res = adminService.create(req);
        assertEquals("/search", res.getCtaUrl());
        assertEquals("blue", res.getColorTheme());

        ArgumentCaptor<Promotion> cap = ArgumentCaptor.forClass(Promotion.class);
        verify(promotionRepository).save(cap.capture());
        assertEquals("blue", cap.getValue().getColorTheme());
    }

    @Test
    void listActive_usesSortOrderAscCreatedAtDesc() {
        when(promotionRepository.findByIsActiveTrueOrderBySortOrderAscCreatedAtDesc())
                .thenReturn(List.of());
        assertEquals(0, publicService.getActivePromotions().size());
        verify(promotionRepository).findByIsActiveTrueOrderBySortOrderAscCreatedAtDesc();
    }

    @Test
    void create_acceptsHttpCtaUrl() {
        PromotionRequest req = validRequest();
        req.setCtaUrl("http://example.com/promo");
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(inv -> {
            Promotion p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            return p;
        });

        PromotionResponse res = adminService.create(req);
        assertEquals("http://example.com/promo", res.getCtaUrl());
    }

    @Test
    void create_acceptsHttpsCtaUrl() {
        PromotionRequest req = validRequest();
        req.setCtaUrl("https://example.com/promo");
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(inv -> {
            Promotion p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            return p;
        });

        assertEquals("https://example.com/promo", adminService.create(req).getCtaUrl());
    }

    @Test
    void create_persistsImageUrl() {
        PromotionRequest req = validRequest();
        req.setImageUrl("/uploads/promotions/demo.jpg");
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(inv -> {
            Promotion p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            return p;
        });

        PromotionResponse res = adminService.create(req);
        assertEquals("/uploads/promotions/demo.jpg", res.getImageUrl());

        ArgumentCaptor<Promotion> cap = ArgumentCaptor.forClass(Promotion.class);
        verify(promotionRepository).save(cap.capture());
        assertEquals("/uploads/promotions/demo.jpg", cap.getValue().getImageUrl());
    }

    @Test
    void create_blankImageUrlClearsImage() {
        PromotionRequest req = validRequest();
        req.setImageUrl("   ");
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(inv -> {
            Promotion p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            return p;
        });

        assertNull(adminService.create(req).getImageUrl());
    }

    private static PromotionRequest validRequest() {
        PromotionRequest req = new PromotionRequest();
        req.setSubtitle("Ưu đãi");
        req.setTitle("Giảm 20%");
        req.setCtaText("Đặt ngay →");
        req.setCtaUrl("/search");
        req.setColorTheme("red");
        req.setIsActive(true);
        req.setSortOrder(0);
        return req;
    }
}
