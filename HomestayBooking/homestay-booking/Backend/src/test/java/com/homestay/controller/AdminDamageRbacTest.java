package com.homestay.controller;

import com.homestay.security.JwtAuthFilter;
import com.homestay.security.JwtAuthenticationEntryPoint;
import com.homestay.service.AdminDamageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminDamageController.class)
class AdminDamageRbacTest {

    @Autowired MockMvc mvc;

    @MockBean AdminDamageService adminDamageService;
    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void list_withoutAuth_unauthorized() throws Exception {
        mvc.perform(get("/api/admin/damage-reports"))
                .andExpect(status().isUnauthorized());
    }
}
