package com.homestay.controller;

import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.service.DamageReportManagerService;
import com.homestay.support.TestFixtures;
import com.homestay.support.TestSecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ManagerDamageReportV1Controller.class)
@AutoConfigureMockMvc(addFilters = false)
class ManagerDamageReportV1ControllerTest {

    @Autowired MockMvc mvc;

    @MockBean DamageReportManagerService damageReportManagerService;
    @MockBean com.homestay.security.JwtAuthFilter jwtAuthFilter;
    @MockBean com.homestay.security.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void list_requiresPropertyId() throws Exception {
        User manager = TestFixtures.user(User.Role.MANAGER);
        UUID propertyId = UUID.randomUUID();
        when(damageReportManagerService.listForManager(
                any(User.class), eq(propertyId), isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mvc.perform(get("/api/v1/manager/damage-reports")
                        .param("propertyId", propertyId.toString())
                        .with(TestSecurityUtils.userPrincipal(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
