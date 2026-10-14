package com.homestay.controller;

import com.homestay.dto.request.ApproveDamageReportRequest;
import com.homestay.dto.request.RejectDamageReportRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.DamageReportDetailResponse;
import com.homestay.dto.response.DamageReportSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.DamageReport;
import com.homestay.entity.User;
import com.homestay.service.DamageReportManagerService;
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

/** SCR-43 — Damage Report Management (Manager only). */
@RestController
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
public class ManagerDamageReportV1Controller {

    private final DamageReportManagerService damageReportManagerService;

    @GetMapping("/damage-reports")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<DamageReportSummaryResponse>>> list(
            @AuthenticationPrincipal User currentUser,
            @RequestParam UUID propertyId,
            @RequestParam(required = false) DamageReport.Status status,
            @RequestParam(required = false) Boolean escalated,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<DamageReportSummaryResponse> data = damageReportManagerService.listForManager(
                currentUser, propertyId, status, escalated, search, page, size);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/damage-reports/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<DamageReportDetailResponse>> detail(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {

        DamageReportDetailResponse data = damageReportManagerService.getDetailForManager(currentUser, id);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PatchMapping("/damage-reports/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<DamageReportDetailResponse>> approve(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody ApproveDamageReportRequest body) {

        DamageReportDetailResponse data = damageReportManagerService.approveForManager(currentUser, id, body);
        String msg = Boolean.TRUE.equals(data.getRequiresAdminEscalation())
                ? "Đã chuyển Admin duyệt" : "Duyệt bồi thường thành công";
        return ResponseEntity.ok(ApiResponse.ok(msg, data));
    }

    @PatchMapping("/damage-reports/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<DamageReportDetailResponse>> reject(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody RejectDamageReportRequest body) {

        DamageReportDetailResponse data = damageReportManagerService.rejectForManager(currentUser, id, body);
        return ResponseEntity.ok(ApiResponse.ok("Đã từ chối báo cáo hư hại", data));
    }
}
