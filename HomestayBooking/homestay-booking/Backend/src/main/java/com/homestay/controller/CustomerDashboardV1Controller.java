package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.CustomerDashboardResponse;
import com.homestay.entity.User;
import com.homestay.service.CustomerDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** SCR-15 — Customer Dashboard composite read (api-spec v1). */
@RestController
@RequestMapping("/api/v1/customer")
public class CustomerDashboardV1Controller {

    private final CustomerDashboardService customerDashboardService;

    public CustomerDashboardV1Controller(CustomerDashboardService customerDashboardService) {
        this.customerDashboardService = customerDashboardService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CustomerDashboardResponse>> getDashboard(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(
                customerDashboardService.getDashboard(currentUser)));
    }
}
