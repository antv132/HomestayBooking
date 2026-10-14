package com.homestay.controller;

import com.homestay.dto.response.AdminDamageReportResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.service.AdminDamageService;
import com.homestay.support.TestFixtures;
import com.homestay.support.TestSecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminDamageController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminDamageControllerTest {

    @Autowired MockMvc mvc;

    @MockBean AdminDamageService adminDamageService;
    @MockBean com.homestay.security.JwtAuthFilter jwtAuthFilter;
    @MockBean com.homestay.security.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void listEscalated_returns200() throws Exception {
        User admin = TestFixtures.user(User.Role.ADMIN);
        PageResponse<AdminDamageReportResponse> page = new PageResponse<>(List.of(), 0, 10, 0, 0);
        when(adminDamageService.listEscalated(any())).thenReturn(page);

        mvc.perform(get("/api/admin/damage-reports")
                        .param("status", "ESCALATED")
                        .with(TestSecurityUtils.userPrincipal(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void coApprove_returnsSuccess() throws Exception {
        User admin = TestFixtures.user(User.Role.ADMIN);
        UUID id = UUID.randomUUID();
        AdminDamageReportResponse body = AdminDamageReportResponse.builder()
                .id(id.toString())
                .status("APPROVED")
                .totalFee(new BigDecimal("6000000"))
                .items(List.of())
                .attachments(List.of())
                .build();
        when(adminDamageService.coApprove(eq(id), eq(new BigDecimal("6000000")), any(User.class)))
                .thenReturn(body);

        mvc.perform(patch("/api/admin/damage-reports/{id}/co-approve", id)
                        .with(csrf())
                        .with(TestSecurityUtils.userPrincipal(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approvedFee\":6000000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
