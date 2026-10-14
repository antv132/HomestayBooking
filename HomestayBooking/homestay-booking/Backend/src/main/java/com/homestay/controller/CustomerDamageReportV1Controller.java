package com.homestay.controller;

import com.homestay.dto.request.DisputeDamageReportRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.CustomerDamageReportResponse;
import com.homestay.entity.User;
import com.homestay.service.CustomerDamageReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 023 FR-013 / US6 — Customer damage reports + dispute.
 * Paths align with contracts/inspection-damage-api.yaml under /api/v1.
 */
@RestController
@RequestMapping("/api/v1/customer/damage-reports")
@RequiredArgsConstructor
public class CustomerDamageReportV1Controller {

    private final CustomerDamageReportService customerDamageReportService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<CustomerDamageReportResponse>>> list(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(customerDamageReportService.listForCustomer(currentUser)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CustomerDamageReportResponse>> detail(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(customerDamageReportService.getForCustomer(currentUser, id)));
    }

    @PatchMapping("/{id}/dispute")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CustomerDamageReportResponse>> dispute(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody DisputeDamageReportRequest body) {
        CustomerDamageReportResponse data = customerDamageReportService.dispute(currentUser, id, body);
        return ResponseEntity.ok(ApiResponse.ok("Đã gửi tranh chấp — chờ Admin xử lý", data));
    }
}
