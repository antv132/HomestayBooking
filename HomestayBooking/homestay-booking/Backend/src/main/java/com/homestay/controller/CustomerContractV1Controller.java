package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.ContractDetailResponse;
import com.homestay.dto.response.ContractSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.service.ContractService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * SCR-21 — My Contract List + Detail (GET /api/v1/customers/me/contracts[/{id}]).
 */
@RestController
@RequestMapping("/api/v1/customers/me")
public class CustomerContractV1Controller {

    private final ContractService contractService;

    public CustomerContractV1Controller(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping("/contracts")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PageResponse<ContractSummaryResponse>>> getMyContracts(
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

    @GetMapping("/contracts/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ContractDetailResponse>> getMyContractDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        ContractDetailResponse data = contractService.getContractDetail(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
