package com.homestay.controller;

import com.homestay.dto.request.UpdateMaintenanceStatusRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.EmployeeMaintenanceTicketResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.service.EmployeeMaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** SCR-61 — Employee Maintenance Workspace. Resource /api/v1/employees/maintenance. */
@RestController
@RequestMapping("/api/v1/employees/maintenance")
@PreAuthorize("hasRole('EMPLOYEE')")
@RequiredArgsConstructor
public class EmployeeMaintenanceController {

    private final EmployeeMaintenanceService employeeMaintenanceService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EmployeeMaintenanceTicketResponse>>> list(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String status) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageResponse<EmployeeMaintenanceTicketResponse> data =
                employeeMaintenanceService.list(currentUser, status, PageRequest.of(safePage, safeSize));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<EmployeeMaintenanceTicketResponse>> updateStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMaintenanceStatusRequest request) {
        EmployeeMaintenanceTicketResponse data =
                employeeMaintenanceService.updateStatus(currentUser, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành công", data));
    }
}
