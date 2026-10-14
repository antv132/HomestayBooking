package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.InvoiceSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.service.InvoiceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** SCR-36 — Manager invoices by property (FR-11). */
@RestController
@RequestMapping("/api/v1/manager/invoices")
@RequiredArgsConstructor
public class ManagerInvoiceController {

    private final InvoiceQueryService invoiceQueryService;

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<InvoiceSummaryResponse>>> list(
            @AuthenticationPrincipal User currentUser,
            @RequestParam UUID propertyId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                invoiceQueryService.listForManager(currentUser, propertyId, status, page, size)));
    }
}
