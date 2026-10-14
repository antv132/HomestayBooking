package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.InvoiceDetailResponse;
import com.homestay.dto.response.InvoiceSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.service.InvoiceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** SCR-26 — Customer invoice history (FR-11). */
@RestController
@RequestMapping("/api/v1/invoices/me")
@RequiredArgsConstructor
public class CustomerInvoiceController {

    private final InvoiceQueryService invoiceQueryService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PageResponse<InvoiceSummaryResponse>>> list(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                invoiceQueryService.listForCustomer(currentUser.getId(), status, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<InvoiceDetailResponse>> detail(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                invoiceQueryService.getDetailForCustomer(id, currentUser.getId())));
    }
}
