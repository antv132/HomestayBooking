package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.PaymentSummaryResponse;
import com.homestay.entity.User;
import com.homestay.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** SCR-26 — Customer payment history (api-spec v1). */
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerPaymentV1Controller {

    private final PaymentService paymentService;

    public CustomerPaymentV1Controller(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/me/payments")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PageResponse<PaymentSummaryResponse>>> getMyPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal User currentUser) {
        PageResponse<PaymentSummaryResponse> data =
                paymentService.getMyPayments(currentUser, page, size, status);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
