package com.homestay.controller;

import com.homestay.dto.request.AdminUpdateComplaintStatusRequest;
import com.homestay.dto.request.ResolveComplaintRequest;
import com.homestay.dto.response.AdminComplaintResponse;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.ComplaintDetailResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.service.AdminComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping({"/api/admin/complaints", "/api/v1/admin/complaints"})
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminComplaintController {

    private final AdminComplaintService adminComplaintService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminComplaintResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize);
        return ResponseEntity.ok(ApiResponse.ok(
                adminComplaintService.listComplaints(status, keyword, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ComplaintDetailResponse>> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminComplaintService.getDetail(id)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminComplaintResponse>> updateStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateComplaintStatusRequest body) {
        String resolution = body.getResolution() != null ? body.getResolution() : body.getResolutionNotes();
        return ResponseEntity.ok(ApiResponse.ok(
                "Cập nhật trạng thái thành công",
                adminComplaintService.updateStatus(
                        id, body.getStatus(), resolution, currentUser)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminComplaintResponse>> patchStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateComplaintStatusRequest body) {
        return updateStatus(currentUser, id, body);
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<AdminComplaintResponse>> resolve(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody ResolveComplaintRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Giải quyết khiếu nại thành công",
                adminComplaintService.resolve(id, body.getResolution(), currentUser)));
    }
}
