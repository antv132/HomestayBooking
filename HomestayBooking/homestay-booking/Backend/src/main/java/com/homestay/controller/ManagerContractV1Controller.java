package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.ContractDetailResponse;
import com.homestay.dto.response.ContractSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
 * SCR-38 — Manager contracts.
 * Canonical: {@code /api/v1/managers/contracts}; alias {@code /api/v1/manager/contracts} (OpenAPI).
 */
@RestController
@RequestMapping({"/api/v1/managers/contracts", "/api/v1/manager/contracts"})
@RequiredArgsConstructor
public class ManagerContractV1Controller {

    private final ContractService contractService;

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<ContractSummaryResponse>>> list(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort) {

        PageResponse<ContractSummaryResponse> data = contractService.getManagerContracts(
                currentUser, propertyId, page, size, status, search, sort);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ContractDetailResponse>> getDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(contractService.getContractDetail(id, currentUser)));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        byte[] pdfBytes = contractService.downloadContractPdf(id, currentUser);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"Contract_" + id + ".pdf\"");
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    @PostMapping("/{id}/resend")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> resend(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @RequestBody(required = false) Map<String, String> request) {
        String targetEmail = request != null ? request.get("email") : null;
        contractService.resendContractEmail(id, currentUser, targetEmail);
        return ResponseEntity.accepted().body(ApiResponse.ok("Đã xếp hàng gửi lại email hợp đồng"));
    }
}
