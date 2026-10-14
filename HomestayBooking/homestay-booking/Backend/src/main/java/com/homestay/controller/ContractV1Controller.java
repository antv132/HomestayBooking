package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.ContractDetailResponse;
import com.homestay.dto.response.ContractSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.service.ContractService;
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
 * Contract v1 endpoints shared by Customer (SCR-21) and Manager (SCR-38).
 * List (customer canonical): GET /api/v1/customers/me/contracts
 * List (manager canonical): GET /api/v1/managers/contracts
 */
@RestController
@RequestMapping("/api/v1/contracts")
public class ContractV1Controller {

    private final ContractService contractService;

    public ContractV1Controller(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PageResponse<ContractSummaryResponse>>> getMyContractsAlias(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal User currentUser) {
        PageResponse<ContractSummaryResponse> data =
                contractService.getMyContracts(currentUser, page, size, status, search, sort);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ContractDetailResponse>> getContractByBooking(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(
                contractService.getContractByBookingId(bookingId, currentUser)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ContractDetailResponse>> getContractDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        ContractDetailResponse data = contractService.getContractDetail(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<byte[]> downloadContractPdf(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        byte[] pdfBytes = contractService.downloadContractPdf(id, currentUser);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"Contract_" + id + ".pdf\"");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @PostMapping("/{id}/resend")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> resendContractEmail(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @RequestBody(required = false) Map<String, String> request) {
        String targetEmail = request != null ? request.get("email") : null;
        contractService.resendContractEmail(id, currentUser, targetEmail);
        return ResponseEntity.accepted().body(ApiResponse.ok("Đã xếp hàng gửi lại email hợp đồng"));
    }
}
