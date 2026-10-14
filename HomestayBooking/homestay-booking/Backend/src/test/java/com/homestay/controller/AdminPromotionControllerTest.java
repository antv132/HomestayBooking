package com.homestay.controller;

import com.homestay.entity.User;
import com.homestay.service.AdminPromotionService;
import com.homestay.support.TestFixtures;
import com.homestay.support.TestSecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract: DELETE → 204 No Content. Role checks covered by {@link AdminPromotionRbacTest}. */
@WebMvcTest(controllers = AdminPromotionController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminPromotionControllerTest {

    @Autowired MockMvc mvc;

    @MockBean AdminPromotionService adminPromotionService;
    @MockBean com.homestay.security.JwtAuthFilter jwtAuthFilter;
    @MockBean com.homestay.security.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void delete_returns204() throws Exception {
        User admin = TestFixtures.user(User.Role.ADMIN);
        UUID id = UUID.randomUUID();

        mvc.perform(delete("/api/v1/admin/promotions/" + id)
                        .with(TestSecurityUtils.userPrincipal(admin)))
                .andExpect(status().isNoContent());

        verify(adminPromotionService).deleteForAdmin(id);
    }
}
