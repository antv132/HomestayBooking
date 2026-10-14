package com.homestay.controller;

import com.homestay.dto.request.AssignEmployeeRequest;
import com.homestay.dto.request.CreateEmployeeRequest;
import com.homestay.dto.request.ReassignEmployeeRequest;
import com.homestay.dto.request.UpdateEmployeeRequest;
import com.homestay.dto.request.UpdateEmployeeStatusRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.EmployeeSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.service.EmployeeManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** FR-20 SCR-39 — Admin employee management v1. */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminEmployeeV1Controller {

    private final EmployeeManagementService employeeManagementService;

    @GetMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeSummaryResponse>>> list(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<EmployeeSummaryResponse> data = employeeManagementService.listAdmin(
                currentUser, propertyId, search, page, size);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/employees/unassigned")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeSummaryResponse>>> listUnassigned(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<EmployeeSummaryResponse> data = employeeManagementService.listUnassigned(
                currentUser, null, search, page, size);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/employees/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeSummaryResponse>> assign(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody AssignEmployeeRequest body) {
        EmployeeSummaryResponse data = employeeManagementService.assignEmployee(currentUser, body);
        return ResponseEntity.ok(ApiResponse.ok("Gán nhân viên thành công", data));
    }

    @PostMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeSummaryResponse>> create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateEmployeeRequest body) {
        EmployeeSummaryResponse data = employeeManagementService.createEmployee(currentUser, body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo nhân viên thành công", data));
    }

    @PutMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeSummaryResponse>> update(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEmployeeRequest body) {
        EmployeeSummaryResponse data = employeeManagementService.updateEmployeeAdmin(
                currentUser, id, body);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật nhân viên thành công", data));
    }

    @PatchMapping("/employees/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeSummaryResponse>> updateStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEmployeeStatusRequest body) {
        EmployeeSummaryResponse data = employeeManagementService.updateEmployeeStatusAdmin(
                currentUser, id, body);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành công", data));
    }

    @PatchMapping("/employees/{id}/reassign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeSummaryResponse>> reassign(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody ReassignEmployeeRequest body) {
        EmployeeSummaryResponse data = employeeManagementService.reassignEmployee(
                currentUser, id, body);
        return ResponseEntity.ok(ApiResponse.ok("Chuyển homestay thành công", data));
    }
}
