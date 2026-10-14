package com.homestay.controller;



import com.homestay.dto.request.AssignEmployeeRequest;

import com.homestay.dto.request.CreateEmployeeRequest;

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



/** SCR-39 — Manager employee management v1. */

@RestController

@RequestMapping("/api/v1/manager")

@RequiredArgsConstructor

public class ManagerEmployeeV1Controller {



    private final EmployeeManagementService employeeManagementService;



    @GetMapping("/employees")

    @PreAuthorize("hasRole('MANAGER')")

    public ResponseEntity<ApiResponse<PageResponse<EmployeeSummaryResponse>>> list(

            @AuthenticationPrincipal User currentUser,

            @RequestParam UUID propertyId,

            @RequestParam(required = false) String search,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "20") int size) {

        PageResponse<EmployeeSummaryResponse> data = employeeManagementService.listByProperty(

                currentUser, propertyId, search, page, size);

        return ResponseEntity.ok(ApiResponse.ok(data));

    }



    @GetMapping("/employees/unassigned")

    @PreAuthorize("hasRole('MANAGER')")

    public ResponseEntity<ApiResponse<PageResponse<EmployeeSummaryResponse>>> listUnassigned(

            @AuthenticationPrincipal User currentUser,

            @RequestParam UUID propertyId,

            @RequestParam(required = false) String search,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "20") int size) {

        PageResponse<EmployeeSummaryResponse> data = employeeManagementService.listUnassigned(

                currentUser, propertyId, search, page, size);

        return ResponseEntity.ok(ApiResponse.ok(data));

    }



    @PostMapping("/employees/assign")

    @PreAuthorize("hasRole('MANAGER')")

    public ResponseEntity<ApiResponse<EmployeeSummaryResponse>> assign(

            @AuthenticationPrincipal User currentUser,

            @Valid @RequestBody AssignEmployeeRequest body) {

        EmployeeSummaryResponse data = employeeManagementService.assignEmployee(currentUser, body);

        return ResponseEntity.ok(ApiResponse.ok("Gán nhân viên thành công", data));

    }



    @PostMapping("/employees")

    @PreAuthorize("hasRole('MANAGER')")

    public ResponseEntity<ApiResponse<EmployeeSummaryResponse>> create(

            @AuthenticationPrincipal User currentUser,

            @Valid @RequestBody CreateEmployeeRequest body) {

        EmployeeSummaryResponse data = employeeManagementService.createEmployee(currentUser, body);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Tạo nhân viên thành công", data));

    }



    @PutMapping("/employees/{id}")

    @PreAuthorize("hasRole('MANAGER')")

    public ResponseEntity<ApiResponse<EmployeeSummaryResponse>> update(

            @AuthenticationPrincipal User currentUser,

            @PathVariable UUID id,

            @RequestParam UUID propertyId,

            @Valid @RequestBody UpdateEmployeeRequest body) {

        EmployeeSummaryResponse data = employeeManagementService.updateEmployee(

                currentUser, id, propertyId, body);

        return ResponseEntity.ok(ApiResponse.ok("Cập nhật nhân viên thành công", data));

    }



    @PatchMapping("/employees/{id}/status")

    @PreAuthorize("hasRole('MANAGER')")

    public ResponseEntity<ApiResponse<EmployeeSummaryResponse>> updateStatus(

            @AuthenticationPrincipal User currentUser,

            @PathVariable UUID id,

            @RequestParam UUID propertyId,

            @Valid @RequestBody UpdateEmployeeStatusRequest body) {

        EmployeeSummaryResponse data = employeeManagementService.updateEmployeeStatus(

                currentUser, id, propertyId, body);

        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành công", data));

    }

}

