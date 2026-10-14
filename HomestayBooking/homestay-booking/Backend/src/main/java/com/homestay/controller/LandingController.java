package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.FeaturedPropertyResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.PromotionResponse;
import com.homestay.dto.response.RoomCalendarResponse;
import com.homestay.dto.response.RoomDetailResponse;
import com.homestay.dto.response.RoomSummaryResponse;
import com.homestay.exception.BusinessException;
import com.homestay.service.PromotionService;
import com.homestay.service.PropertyService;
import com.homestay.service.RoomService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Public APIs — SCR-01 landing, SCR-07 room search (api-spec v1).
 */
@RestController
@RequestMapping("/api/v1")
public class LandingController {

    private final PromotionService promotionService;
    private final PropertyService propertyService;
    private final RoomService roomService;

    public LandingController(PromotionService promotionService,
                             PropertyService propertyService,
                             RoomService roomService) {
        this.promotionService = promotionService;
        this.propertyService = propertyService;
        this.roomService = roomService;
    }

    @GetMapping("/promotions/active")
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> getActivePromotions() {
        return ResponseEntity.ok(ApiResponse.ok(promotionService.getActivePromotions()));
    }

    @GetMapping("/properties/featured")
    public ResponseEntity<ApiResponse<List<FeaturedPropertyResponse>>> getFeaturedProperties(
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(propertyService.getFeatured(limit)));
    }

    @GetMapping("/rooms/featured")
    public ResponseEntity<ApiResponse<List<RoomSummaryResponse>>> getFeaturedRooms(
            @RequestParam(defaultValue = "8") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(roomService.getFeatured(limit)));
    }

    @GetMapping("/rooms/price-stats")
    public ResponseEntity<ApiResponse<java.util.Map<String, BigDecimal>>> getPriceStats() {
        return ResponseEntity.ok(ApiResponse.ok(roomService.getPriceStats()));
    }

    /** SCR-07 — public room search & listing (contract: GET /api/v1/rooms; alias /rooms/search). */
    @GetMapping({"/rooms", "/rooms/search"})
    public ResponseEntity<ApiResponse<PageResponse<RoomSummaryResponse>>> searchRooms(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String propertyId,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        Integer guestCapacity = guests != null ? guests : capacity;
        // With date filters: do not force Room.Status=AVAILABLE (inventory is date-range based).
        // Without dates: default AVAILABLE for simple browse.
        boolean hasDates = checkIn != null && checkOut != null;
        String roomStatus;
        if (status != null && !status.isBlank()) {
            roomStatus = status;
        } else if (hasDates) {
            roomStatus = null;
        } else {
            roomStatus = "AVAILABLE";
        }

        String[] sortParts = sort.split(",");
        Sort.Direction dir = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = sortParts[0].trim();
        PageRequest pageable = PageRequest.of(page, size, Sort.by(dir, sortField));

        return ResponseEntity.ok(ApiResponse.ok(
                roomService.getAll(search, location, roomStatus, propertyId,
                        roomType, minPrice, maxPrice, guestCapacity, checkIn, checkOut, pageable)));
    }

    /** SCR-08 — room detail (public). */
    @GetMapping("/rooms/{id}")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> getRoomById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(roomService.getById(id)));
    }

    /** SCR-08 — published reviews for a room. */
    @GetMapping("/rooms/{id}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<RoomDetailResponse.ReviewInfo>>> getRoomReviews(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                roomService.getPublishedReviews(id, PageRequest.of(page, size))));
    }

    /** SCR-08 — mini calendar data. */
    @GetMapping("/rooms/{id}/calendar")
    public ResponseEntity<ApiResponse<RoomCalendarResponse>> getRoomCalendar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(roomService.getCalendar(id)));
    }

    /** SCR-08/SCR-09 — availability: month view (startDate+endDate) or range check (checkIn+checkOut). */
    @GetMapping("/rooms/{id}/availability")
    public ResponseEntity<ApiResponse<?>> getRoomAvailability(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {

        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(ApiResponse.ok(roomService.getMonthAvailability(id, startDate, endDate)));
        }
        if (checkIn != null && checkOut != null) {
            return ResponseEntity.ok(ApiResponse.ok(roomService.checkAvailability(id, checkIn, checkOut)));
        }
        throw new BusinessException("Cần startDate+endDate (lịch tháng) hoặc checkIn+checkOut (kiểm tra khoảng ngày)");
    }
}
