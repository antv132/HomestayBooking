package com.homestay.controller;

import com.homestay.dto.response.AvailabilityResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.RoomCalendarResponse;
import com.homestay.dto.response.RoomDetailResponse;
import com.homestay.service.PromotionService;
import com.homestay.service.PropertyService;
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

@WebMvcTest(controllers = LandingController.class)
@AutoConfigureMockMvc(addFilters = false)
class LandingControllerTest {

    @Autowired MockMvc mvc;

    @MockBean PromotionService promotionService;
    @MockBean PropertyService propertyService;
    @MockBean RoomService roomService;
    @MockBean com.homestay.security.JwtAuthFilter jwtAuthFilter;
    @MockBean com.homestay.security.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void activePromotions() throws Exception {
        when(promotionService.getActivePromotions()).thenReturn(List.of());
        mvc.perform(get("/api/v1/promotions/active")).andExpect(status().isOk());
    }

    @Test
    void featuredPropertiesAndRooms() throws Exception {
        when(propertyService.getFeatured(anyInt())).thenReturn(List.of());
        when(roomService.getFeatured(anyInt())).thenReturn(List.of());
        mvc.perform(get("/api/v1/properties/featured")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/rooms/featured")).andExpect(status().isOk());
    }

    @Test
    void priceStats() throws Exception {
        when(roomService.getPriceStats()).thenReturn(Map.of(
                "minPrice", BigDecimal.ZERO, "maxPrice", new BigDecimal("1000")));
        mvc.perform(get("/api/v1/rooms/price-stats")).andExpect(status().isOk());
    }

    @Test
    void searchRooms_defaultsAvailable() throws Exception {
        when(roomService.getAll(any(), any(), eq("AVAILABLE"), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 12, 0, 0));
        mvc.perform(get("/api/v1/rooms")).andExpect(status().isOk());
    }

    @Test
    void searchRooms_withDates() throws Exception {
        when(roomService.getAll(any(), any(), eq(null), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 12, 0, 0));
        mvc.perform(get("/api/v1/rooms/search")
                        .param("checkIn", "2026-08-01")
                        .param("checkOut", "2026-08-05")
                        .param("guests", "2"))
                .andExpect(status().isOk());
    }

    @Test
    void roomDetailCalendarReviewsAvailability() throws Exception {
        UUID id = UUID.randomUUID();
        RoomDetailResponse detail = new RoomDetailResponse();
        detail.setId(id);
        when(roomService.getById(id)).thenReturn(detail);
        when(roomService.getCalendar(id)).thenReturn(new RoomCalendarResponse("AVAILABLE", List.of()));
        when(roomService.getPublishedReviews(eq(id), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 5, 0, 0));
        when(roomService.checkAvailability(eq(id), any(), any()))
                .thenReturn(new AvailabilityResponse(true, List.of()));

        mvc.perform(get("/api/v1/rooms/" + id)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/rooms/" + id + "/calendar")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/rooms/" + id + "/reviews")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/rooms/" + id + "/availability")
                        .param("checkIn", "2026-08-01")
                        .param("checkOut", "2026-08-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }
}
