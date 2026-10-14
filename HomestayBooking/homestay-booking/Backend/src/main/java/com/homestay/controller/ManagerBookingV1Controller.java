package com.homestay.controller;

import com.homestay.dto.request.CancelBookingRequest;
import com.homestay.dto.request.ManagerCheckInRequest;
import com.homestay.dto.request.ManagerCheckOutRequest;
import com.homestay.dto.request.ModifyBookingRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.BookingSummaryResponse;
import com.homestay.dto.response.CancellationPreviewResponse;
import com.homestay.dto.response.ManagerBookingDetailResponse;
import com.homestay.dto.response.ModifyBookingResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** SCR-34/35/37/69 — Manager booking list + detail + check-in/out + cancel v1. */
@RestController
@RequestMapping("/api/v1/manager/bookings")
@RequiredArgsConstructor
public class ManagerBookingV1Controller {

    private final BookingService bookingService;

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<BookingSummaryResponse>>> list(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInTo,
            @RequestParam(defaultValue = "checkInDate,desc") String sort) {

        PageResponse<BookingSummaryResponse> data = bookingService.getBookingsForManagerScoped(
                currentUser, propertyId, status, search, checkInFrom, checkInTo, page, size, sort);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ManagerBookingDetailResponse>> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        ManagerBookingDetailResponse data = bookingService.getBookingDetailForManager(currentUser, id);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{id}/cancel/preview")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<CancellationPreviewResponse>> cancelPreview(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        CancellationPreviewResponse data = bookingService.getManagerCancellationPreview(currentUser, id);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /** SCR-69 — Manager cancel (100% refund). Body must include cancelReason or reason. */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ManagerBookingDetailResponse>> cancel(
            @PathVariable UUID id,
            @Valid @RequestBody CancelBookingRequest body,
            @AuthenticationPrincipal User currentUser) {
        String cancelReason = body != null ? body.resolvedReason() : null;
        if (cancelReason == null || cancelReason.isBlank()) {
            throw new BusinessException("Lý do hủy (cancelReason) là bắt buộc");
        }

        ManagerBookingDetailResponse data = bookingService.cancelBookingForManager(currentUser, id, cancelReason);
        return ResponseEntity.ok(ApiResponse.ok("Hủy đặt phòng thành công — hoàn 100% số tiền đã thanh toán", data));
    }

    @PostMapping("/{id}/id-documents")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<String>>> uploadIdDocuments(
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal User currentUser) {
        List<String> urls = bookingService.uploadIdDocumentsForManager(currentUser, id, files);
        return ResponseEntity.ok(ApiResponse.ok("Tải ảnh giấy tờ thành công", urls));
    }

    /** FR-015 — Modify Confirmed booking (dates / room). */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ModifyBookingResponse>> modify(
            @PathVariable UUID id,
            @Valid @RequestBody ModifyBookingRequest body,
            @AuthenticationPrincipal User currentUser) {
        ModifyBookingResponse data = bookingService.modifyBookingForManager(currentUser, id, body);
        return ResponseEntity.ok(ApiResponse.ok(data.getMessage(), data));
    }

    @PatchMapping("/{id}/check-in")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ManagerBookingDetailResponse>> checkIn(
            @PathVariable UUID id,
            @Valid @RequestBody ManagerCheckInRequest body,
            @AuthenticationPrincipal User currentUser) {
        ManagerBookingDetailResponse data = bookingService.markAsCheckedInForManager(currentUser, id, body);
        return ResponseEntity.ok(ApiResponse.ok("Nhận phòng thành công", data));
    }

    @PatchMapping("/{id}/check-out")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ManagerBookingDetailResponse>> checkOut(
            @PathVariable UUID id,
            @Valid @RequestBody ManagerCheckOutRequest body,
            @AuthenticationPrincipal User currentUser) {
        ManagerBookingDetailResponse data = bookingService.markAsCheckedOutForManager(currentUser, id, body);
        String message = "CHECKED_OUT".equals(data.getStatus())
                ? "Trả phòng thành công"
                : "Đã chuyển sang chờ kiểm tra phòng";
        return ResponseEntity.ok(ApiResponse.ok(message, data));
    }
}
