package com.homestay.controller;

import com.homestay.security.JwtAuthFilter;
import com.homestay.security.JwtAuthenticationEntryPoint;
import com.homestay.service.AdminPromotionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FR-009 / SC-005 — unauthenticated access denied. */
@WebMvcTest(controllers = AdminPromotionController.class)
class AdminPromotionRbacTest {

    @Autowired MockMvc mvc;

    @MockBean AdminPromotionService adminPromotionService;
    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void list_withoutAuth_unauthorized() throws Exception {
        mvc.perform(get("/api/v1/admin/promotions"))
                .andExpect(status().isUnauthorized());
    }
}
