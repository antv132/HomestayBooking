package com.homestay.controller;

import com.homestay.config.VNPayConfig;
import com.homestay.dto.response.ApiResponse;
import com.homestay.entity.Payment;
import com.homestay.entity.User;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.service.PaymentService;
import com.homestay.service.VNPayService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class CustomerPaymentController {

    private final VNPayService vnPayService;
    private final PaymentService paymentService;
    private final VNPayConfig vnPayConfig;

    public CustomerPaymentController(
            VNPayService vnPayService,
            PaymentService paymentService,
            VNPayConfig vnPayConfig) {
        this.vnPayService = vnPayService;
        this.paymentService = paymentService;
        this.vnPayConfig = vnPayConfig;
    }

    @PostMapping("/vnpay/create-url")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> createPaymentUrl(
            @RequestParam UUID bookingId,
            @RequestParam String type,
            @AuthenticationPrincipal User currentUser) {

        Map<String, String> result = paymentService.createVnpayPaymentUrl(bookingId, type, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Tạo URL thanh toán thành công", result));
    }

    @GetMapping("/vnpay/return")
    public void vnpayReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty()) && fieldName.startsWith("vnp_")) {
                fields.put(fieldName, fieldValue);
            }
        }

        String paymentIdStr = request.getParameter("paymentId");
        String vnp_TxnRef = request.getParameter("vnp_TxnRef");
        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        String vnp_TransactionNo = request.getParameter("vnp_TransactionNo");
        String frontendRedirectUrl = vnPayConfig.getFrontendResultUrl();

        UUID paymentId;
        try {
            paymentId = paymentService.resolvePaymentIdForReturn(paymentIdStr, vnp_TxnRef);
        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            response.sendRedirect(frontendRedirectUrl + "?status=failed&message=Missing_Payment_ID");
            return;
        }

        if (!vnPayService.verifySignature(fields)) {
            response.sendRedirect(frontendRedirectUrl + "?status=failed&message=Invalid_Signature");
            return;
        }

        try {
            boolean success = "00".equals(vnp_ResponseCode);
            Payment payment = paymentService.completeVnpayReturn(
                    paymentId, success, vnp_ResponseCode, vnp_TransactionNo);
            if (success) {
                response.sendRedirect(frontendRedirectUrl + "?status=success&bookingId=" + payment.getBooking().getId());
            } else {
                response.sendRedirect(frontendRedirectUrl + "?status=failed&message=Payment_Failed");
            }
        } catch (ResourceNotFoundException e) {
            response.sendRedirect(frontendRedirectUrl + "?status=failed&message=Payment_Not_Found");
        }
    }
}
