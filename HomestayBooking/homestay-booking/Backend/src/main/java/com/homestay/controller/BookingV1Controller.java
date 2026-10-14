package com.homestay.controller;

import com.homestay.dto.request.CancelBookingRequest;
import com.homestay.dto.request.CreateBookingRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.BookingDetailResponse;
import com.homestay.dto.response.BookingSummaryResponse;
import com.homestay.dto.response.CancellationPreviewResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.exception.ForbiddenException;
import com.homestay.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** SCR-16/SCR-17/SCR-18/SCR-19 — Customer booking (api-spec v1). */
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingV1Controller {

    private final BookingService bookingService;

    public BookingV1Controller(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            throw new ForbiddenException("Vui lòng đăng nhập để đặt phòng");
        }
        BookingDetailResponse data = bookingService.createBooking(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đặt phòng thành công, vui lòng thanh toán cọc", data));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PageResponse<BookingSummaryResponse>>> getMyBookings(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        if (currentUser == null) {
            throw new ForbiddenException("Vui lòng đăng nhập để xem đặt phòng");
        }
        PageResponse<BookingSummaryResponse> data =
                bookingService.getMyBookings(currentUser, page, size, status, sort);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/me/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingDetailResponse>> getMyBookingDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            throw new ForbiddenException("Vui lòng đăng nhập để xem chi tiết đặt phòng");
        }
        BookingDetailResponse data = bookingService.getBookingDetail(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/me/{id}/cancel/preview")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CancellationPreviewResponse>> getCancellationPreview(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            throw new ForbiddenException("Vui lòng đăng nhập để hủy đặt phòng");
        }
        CancellationPreviewResponse data = bookingService.getCancellationPreview(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PatchMapping("/me/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> cancelMyBooking(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @RequestBody(required = false) CancelBookingRequest request) {
        if (currentUser == null) {
            throw new ForbiddenException("Vui lòng đăng nhập để hủy đặt phòng");
        }
        String reason = request != null ? request.resolvedReason() : null;
        bookingService.cancelBooking(id, currentUser, reason);
        return ResponseEntity.ok(ApiResponse.ok("Hủy đặt phòng thành công"));
    }
}
