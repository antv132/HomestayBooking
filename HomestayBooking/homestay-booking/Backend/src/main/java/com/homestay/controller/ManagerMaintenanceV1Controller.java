package com.homestay.controller;

import com.homestay.dto.request.AssignMaintenanceTaskRequest;
import com.homestay.dto.request.UpdateMaintenanceStatusRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.MaintenanceTaskSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.MaintenanceTicket;
import com.homestay.entity.User;
import com.homestay.service.MaintenanceTaskManagerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** SCR-41 — Maintenance Tasks (Manager only). Không đụng legacy /api/maintenance-tickets. */
@RestController
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
public class ManagerMaintenanceV1Controller {

    private final MaintenanceTaskManagerService maintenanceTaskManagerService;

    @GetMapping("/maintenance-tasks")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<MaintenanceTaskSummaryResponse>>> list(
            @AuthenticationPrincipal User currentUser,
            @RequestParam UUID propertyId,
            @RequestParam(required = false) MaintenanceTicket.Status status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<MaintenanceTaskSummaryResponse> data = maintenanceTaskManagerService.listForManager(
                currentUser, propertyId, status, search, page, size);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PatchMapping("/maintenance-tasks/{id}/assign")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<MaintenanceTaskSummaryResponse>> assign(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody AssignMaintenanceTaskRequest body) {

        MaintenanceTaskSummaryResponse data = maintenanceTaskManagerService.assignForManager(currentUser, id, body);
        return ResponseEntity.ok(ApiResponse.ok("Gán kỹ thuật viên thành công", data));
    }

    @PatchMapping("/maintenance-tasks/{id}/status")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<MaintenanceTaskSummaryResponse>> updateStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMaintenanceStatusRequest body) {

        MaintenanceTaskSummaryResponse data = maintenanceTaskManagerService.updateStatusForManager(currentUser, id, body);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành công", data));
    }
}
