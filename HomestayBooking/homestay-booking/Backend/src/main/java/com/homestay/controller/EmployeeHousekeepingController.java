package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.EmployeeHousekeepingTaskResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.service.EmployeeHousekeepingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** SCR-60 - Employee Housekeeping Workspace. Resource /api/v1/employees/housekeeping. */
@RestController
@RequestMapping("/api/v1/employees/housekeeping")
@PreAuthorize("hasRole('EMPLOYEE')")
@RequiredArgsConstructor
public class EmployeeHousekeepingController {

    private final EmployeeHousekeepingService employeeHousekeepingService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EmployeeHousekeepingTaskResponse>>> list(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String status) {
        PageResponse<EmployeeHousekeepingTaskResponse> data =
                employeeHousekeepingService.list(currentUser, status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<Void>> start(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {
        employeeHousekeepingService.start(currentUser, id);
        return ResponseEntity.ok(ApiResponse.ok("Bat dau don phong"));
    }

    @PostMapping("/{id}/finish")
    public ResponseEntity<ApiResponse<Void>> finish(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {
        employeeHousekeepingService.finish(currentUser, id);
        return ResponseEntity.ok(ApiResponse.ok("Hoan thanh don phong"));
    }
}