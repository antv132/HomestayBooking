package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.BookingSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.exception.ForbiddenException;
import com.homestay.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /** SCR-11 / SCR-18 — danh sách booking của khách hàng đang đăng nhập */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookingSummaryResponse>>> getBookings(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        boolean isManager = currentUser.getRole() == User.Role.MANAGER;

        if (isManager) {
            PageResponse<BookingSummaryResponse> data =
                    bookingService.getAllBookings(page, size, status, search, sort);
            return ResponseEntity.ok(ApiResponse.ok(data));
        } else {
            PageResponse<BookingSummaryResponse> data =
                    bookingService.getMyBookings(currentUser, page, size, status, sort);
            return ResponseEntity.ok(ApiResponse.ok(data));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<com.homestay.dto.response.BookingDetailResponse>> getBookingDetail(
            @org.springframework.web.bind.annotation.PathVariable java.util.UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        com.homestay.dto.response.BookingDetailResponse data = bookingService.getBookingDetail(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('MANAGER')")
    @org.springframework.web.bind.annotation.PatchMapping("/{id}/check-in")
    public ResponseEntity<ApiResponse<Void>> checkIn(@org.springframework.web.bind.annotation.PathVariable java.util.UUID id) {
        throw new com.homestay.exception.BusinessException(
                "Legacy check-in đã bị vô hiệu. Dùng PATCH /api/v1/manager/bookings/{id}/check-in (có xác minh CMND và kiểm tra remaining).");
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('MANAGER')")
    @org.springframework.web.bind.annotation.PatchMapping("/{id}/check-out")
    public ResponseEntity<ApiResponse<Void>> checkOut(@org.springframework.web.bind.annotation.PathVariable java.util.UUID id) {
        throw new com.homestay.exception.BusinessException(
                "Legacy check-out đã bị vô hiệu. Dùng PATCH /api/v1/manager/bookings/{id}/check-out (có cổng inspection / housekeeping).");
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelBooking(
            @org.springframework.web.bind.annotation.PathVariable java.util.UUID id,
            @AuthenticationPrincipal User currentUser) {
        bookingService.cancelBooking(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Hủy booking thành công"));
    }

    @org.springframework.web.bind.annotation.PostMapping
    public ResponseEntity<ApiResponse<com.homestay.dto.response.BookingDetailResponse>> createBooking(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.homestay.dto.request.CreateBookingRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        if (currentUser == null) {
            throw new ForbiddenException("Vui lòng đăng nhập để đặt phòng");
        }
        com.homestay.dto.response.BookingDetailResponse data = bookingService.createBooking(request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Đặt phòng thành công, vui lòng thanh toán cọc", data));
    }
}
