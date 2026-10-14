package com.homestay.controller;

import com.homestay.dto.request.CoApproveDamageRequest;
import com.homestay.dto.response.AdminDamageReportResponse;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.service.AdminDamageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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

/** SCR-53 - Damage Escalation (Admin). Resource /api/admin/damage-reports. */
@RestController
@RequestMapping("/api/admin/damage-reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDamageController {

    private final AdminDamageService adminDamageService;

    // status nhan cho khop FE nhung bo qua: endpoint luon tra hang doi escalated PENDING_APPROVAL.
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminDamageReportResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // Không truyền Sort: query findEscalatedForAdmin đã ORDER BY createdAt DESC.
        // Sort + ORDER BY trùng cột → SQL Server báo lỗi duplicate order by column.
        PageRequest pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(adminDamageService.listEscalated(pageable)));
    }

    @PatchMapping("/{id}/co-approve")
    public ResponseEntity<ApiResponse<AdminDamageReportResponse>> coApprove(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody CoApproveDamageRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Co-approve thanh cong",
                adminDamageService.coApprove(id, body.getApprovedFee(), currentUser)));
    }
}