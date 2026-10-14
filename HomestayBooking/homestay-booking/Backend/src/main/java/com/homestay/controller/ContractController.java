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
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Legacy /api/contracts aliases — prefer /api/v1/... paths.
 * Manager list now property-scopes via {@link ContractService#getManagerContracts}.
 */
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<ContractSummaryResponse>>> getAllContracts(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort
    ) {
        PageResponse<ContractSummaryResponse> data = contractService.getManagerContracts(
                currentUser, propertyId, page, size, status, search, sort);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ContractDetailResponse>> getContractDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        ContractDetailResponse data = contractService.getContractDetail(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<ContractDetailResponse>> getContractByBooking(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal User currentUser
    ) {
        ContractDetailResponse data = contractService.getContractByBookingId(bookingId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('MANAGER', 'CUSTOMER', 'ADMIN')")
    public ResponseEntity<byte[]> downloadContractPdf(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        byte[] pdfBytes = contractService.downloadContractPdf(id, currentUser);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Contract_" + id + ".pdf");
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
            @RequestBody(required = false) Map<String, String> request
    ) {
        String targetEmail = request != null ? request.get("email") : null;
        contractService.resendContractEmail(id, currentUser, targetEmail);
        return ResponseEntity.accepted().body(ApiResponse.ok("Đã xếp hàng gửi lại email hợp đồng"));
    }
}
