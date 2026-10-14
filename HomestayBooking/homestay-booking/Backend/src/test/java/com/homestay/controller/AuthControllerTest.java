package com.homestay.controller;

import com.homestay.dto.request.LoginRequest;
import com.homestay.dto.response.AuthResponse;
import com.homestay.entity.User;
import com.homestay.service.AuthService;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired MockMvc mvc;

    @MockBean AuthService authService;
    @MockBean com.homestay.security.JwtAuthFilter jwtAuthFilter;
    @MockBean com.homestay.security.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void login_returnsTokens() throws Exception {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        AuthResponse resp = new AuthResponse("access", "refresh", AuthResponse.fromUser(user));
        when(authService.login(any(LoginRequest.class))).thenReturn(resp);

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"customer@test.local\",\"password\":\"Secret1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access"));
    }
}
