package com.homestay.controller;

import com.homestay.dto.request.CreateComplaintRequest;
import com.homestay.dto.request.UpdateComplaintStatusRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.ComplaintDetailResponse;
import com.homestay.dto.response.ComplaintSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /** Manager complaint APIs deprecated — Admin SCR-54 owns complaints. */
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/manager/complaints")
    public ResponseEntity<ApiResponse<Void>> getAllComplaintsDeprecated() {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(ApiResponse.error("Manager complaint list deprecated. Use Admin SCR-54."));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/manager/complaints/{id}")
    public ResponseEntity<ApiResponse<Void>> getComplaintDetailDeprecated(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(ApiResponse.error("Manager complaint detail deprecated. Use Admin SCR-54."));
    }

    /** Manager complaint status write deprecated — use Admin path. Returns 410 GONE. */
    @PreAuthorize("hasRole('MANAGER')")
    @PatchMapping("/complaints/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateComplaintStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateComplaintStatusRequest request) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(ApiResponse.error("Manager complaint status update deprecated. Use Admin workflow."));
    }

    // ====== CUSTOMER endpoints (legacy) ======

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/complaints")
    public ResponseEntity<ApiResponse<ComplaintDetailResponse>> submitComplaint(
            @Valid @RequestBody CreateComplaintRequest request,
            @AuthenticationPrincipal User currentUser) {
        ComplaintDetailResponse data = complaintService.submitComplaint(request, currentUser);
        return ResponseEntity.status(201).body(ApiResponse.ok("Gửi khiếu nại thành công", data));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/complaints")
    public ResponseEntity<ApiResponse<List<ComplaintDetailResponse>>> getMyComplaints(
            @AuthenticationPrincipal User currentUser) {
        List<ComplaintDetailResponse> data = complaintService.getMyComplaints(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    // ====== CUSTOMER V1 endpoints ======

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/v1/complaints")
    public ResponseEntity<ApiResponse<ComplaintDetailResponse>> submitComplaintV1(
            @Valid @RequestBody CreateComplaintRequest request,
            @AuthenticationPrincipal User currentUser) {
        ComplaintDetailResponse data = complaintService.submitComplaint(request, currentUser);
        return ResponseEntity.status(201).body(ApiResponse.ok("Gửi khiếu nại thành công", data));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/v1/complaints")
    public ResponseEntity<ApiResponse<PageResponse<ComplaintDetailResponse>>> listMyComplaintsV1(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        PageResponse<ComplaintDetailResponse> data = complaintService.listMyComplaints(
                currentUser.getId(), PageRequest.of(safePage, safeSize));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/v1/complaints/{id}")
    public ResponseEntity<ApiResponse<ComplaintDetailResponse>> getMyComplaintDetailV1(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {
        ComplaintDetailResponse data = complaintService.getMyComplaintDetail(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
