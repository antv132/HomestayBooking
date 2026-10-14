package com.homestay.controller;

import com.homestay.dto.response.AvailabilityResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.RoomCalendarResponse;
import com.homestay.dto.response.RoomDetailResponse;
import com.homestay.dto.response.RoomSummaryResponse;
import com.homestay.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RoomController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoomControllerTest {

    @Autowired MockMvc mvc;

    @MockBean RoomService roomService;
    @MockBean com.homestay.security.JwtAuthFilter jwtAuthFilter;
    @MockBean com.homestay.security.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void getAll_ok() throws Exception {
        when(roomService.getAll(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 12, 0, 0));
        mvc.perform(get("/api/rooms").param("page", "0").param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getPriceStats_ok() throws Exception {
        when(roomService.getPriceStats()).thenReturn(Map.of(
                "minPrice", new BigDecimal("100"),
                "maxPrice", new BigDecimal("900")));
        mvc.perform(get("/api/rooms/price-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.minPrice").value(100));
    }

    @Test
    void getFeatured_ok() throws Exception {
        when(roomService.getFeatured(anyInt())).thenReturn(List.of(new RoomSummaryResponse()));
        mvc.perform(get("/api/rooms/featured").param("limit", "3"))
                .andExpect(status().isOk());
    }

    @Test
    void getById_ok() throws Exception {
        UUID id = UUID.randomUUID();
        RoomDetailResponse detail = new RoomDetailResponse();
        detail.setId(id);
        when(roomService.getById(id)).thenReturn(detail);
        mvc.perform(get("/api/rooms/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    void getCalendar_ok() throws Exception {
        UUID id = UUID.randomUUID();
        when(roomService.getCalendar(id)).thenReturn(new RoomCalendarResponse("AVAILABLE", List.of()));
        mvc.perform(get("/api/rooms/" + id + "/calendar"))
                .andExpect(status().isOk());
    }

    @Test
    void checkAvailability_ok() throws Exception {
        UUID id = UUID.randomUUID();
        when(roomService.checkAvailability(eq(id), any(), any()))
                .thenReturn(new AvailabilityResponse(true, List.of()));
        mvc.perform(get("/api/rooms/" + id + "/availability")
                        .param("checkIn", "2026-08-01")
                        .param("checkOut", "2026-08-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }
}
