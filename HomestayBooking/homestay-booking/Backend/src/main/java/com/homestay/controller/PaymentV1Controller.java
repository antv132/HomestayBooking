package com.homestay.controller;

import com.homestay.dto.request.CreateVnpayPaymentRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.PaymentDetailResponse;
import com.homestay.dto.response.PaymentSummaryResponse;
import com.homestay.entity.User;
import com.homestay.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * SCR-20 Order Review & Payment — Customer VNPay deposit.
 * Also supports remaining-balance URL creation (legacy query contract).
 * US5 contract alias: GET /api/v1/payments/me[+/{id}].
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentV1Controller {

    private final PaymentService paymentService;

    public PaymentV1Controller(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * SCR-20 — create VNPay URL for deposit.
     * Body: { "bookingId": "uuid" }
     */
    @PostMapping("/vnpay")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> createDepositVnpayUrl(
            @Valid @RequestBody CreateVnpayPaymentRequest request,
            @AuthenticationPrincipal User currentUser) {
        Map<String, String> result =
                paymentService.createVnpayPaymentUrl(request.getBookingId(), "DEPOSIT", currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Tạo URL thanh toán thành công", result));
    }

    /** Legacy / remaining balance — query params bookingId + type. */
    @PostMapping("/vnpay/create-url")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> createPaymentUrl(
            @RequestParam UUID bookingId,
            @RequestParam String type,
            @AuthenticationPrincipal User currentUser) {
        Map<String, String> result = paymentService.createVnpayPaymentUrl(bookingId, type, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Tạo URL thanh toán thành công", result));
    }

    /** Contract alias for {@code /api/v1/customers/me/payments}. */
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PageResponse<PaymentSummaryResponse>>> getMyPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(
                paymentService.getMyPayments(currentUser, page, size, status)));
    }

    @GetMapping("/me/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> getMyPaymentDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getPaymentDetail(id, currentUser)));
    }
}
