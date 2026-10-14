package com.homestay.controller;

import com.homestay.dto.request.EmployeeInspectionResultRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.ChecklistItemDefinitionResponse;
import com.homestay.dto.response.EmployeeInspectionResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.service.EmployeeInspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** SCR-62 - Employee Room Inspection Hub. Resource /api/v1/employees/inspections. */
@RestController
@RequestMapping("/api/v1/employees/inspections")
@PreAuthorize("hasRole('EMPLOYEE')")
@RequiredArgsConstructor
public class EmployeeInspectionController {

    private final EmployeeInspectionService employeeInspectionService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EmployeeInspectionResponse>>> list(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageResponse<EmployeeInspectionResponse> data =
                employeeInspectionService.list(currentUser, status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /** Danh mục checklist đang active (global seed). */
    @GetMapping("/checklist-items")
    public ResponseEntity<ApiResponse<List<ChecklistItemDefinitionResponse>>> checklistItems() {
        return ResponseEntity.ok(ApiResponse.ok(employeeInspectionService.listActiveChecklistItems()));
    }

    @PostMapping("/{id}/pass")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pass(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) EmployeeInspectionResultRequest request) {
        employeeInspectionService.pass(currentUser, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Inspection passed", Collections.emptyMap()));
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> fail(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) EmployeeInspectionResultRequest request) {
        employeeInspectionService.fail(currentUser, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Inspection failed", Collections.emptyMap()));
    }
}
