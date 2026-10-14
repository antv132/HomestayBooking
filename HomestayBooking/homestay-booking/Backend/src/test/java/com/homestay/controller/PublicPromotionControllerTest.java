package com.homestay.controller;

import com.homestay.dto.response.PromotionResponse;
import com.homestay.security.JwtAuthFilter;
import com.homestay.security.JwtAuthenticationEntryPoint;
import com.homestay.service.PromotionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicPromotionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicPromotionControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean PromotionService promotionService;
    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void listActive_returnsOnlyWhatServiceProvides() throws Exception {
        PromotionResponse r = new PromotionResponse();
        r.setId(UUID.randomUUID());
        r.setTitle("Summer");
        r.setSubtitle("SALE");
        r.setCtaText("Go");
        r.setCtaUrl("/search");
        r.setColorTheme("red");
        r.setActive(true);
        r.setSortOrder(0);
        when(promotionService.getActivePromotions()).thenReturn(List.of(r));

        mockMvc.perform(get("/api/v1/public/promotions").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Summer"));
    }
}
