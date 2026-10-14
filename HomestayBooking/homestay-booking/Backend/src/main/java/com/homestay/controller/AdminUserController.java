package com.homestay.controller;

import com.homestay.dto.request.AdminCreateManagerRequest;
import com.homestay.dto.request.AdminUpdateManagerRequest;
import com.homestay.dto.request.AdminUpdateUserRequest;
import com.homestay.dto.request.UpdateCustomerStatusRequest;
import com.homestay.dto.response.AdminCustomerBookingSummaryResponse;
import com.homestay.dto.response.AdminUserResponse;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

/**
 * SCR-50/51 - Manager/Customer Directory (Admin).
 * Paths: /api/admin/users and /api/v1/admin/users.
 */
@RestController
@RequestMapping({"/api/admin/users", "/api/v1/admin/users"})
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> list(
            @RequestParam String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(adminUserService.listUsers(role, status, keyword, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminUserService.getById(id)));
    }

    /** SCR-50 — Admin tạo tài khoản Manager. */
    @PostMapping("/managers")
    public ResponseEntity<ApiResponse<AdminUserResponse>> createManager(
            @Valid @RequestBody AdminCreateManagerRequest req) {
        AdminUserResponse data = adminUserService.createManager(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo tài khoản Manager thành công", data));
    }

    /** SCR-50 — Admin sửa tài khoản Manager. */
    @PutMapping("/managers/{id}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateManager(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateManagerRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Cập nhật tài khoản Manager thành công",
                adminUserService.updateManager(id, req)));
    }

    /** SCR-51 — Lịch sử đặt phòng của Customer (Drawer). */
    @GetMapping("/{id}/bookings")
    public ResponseEntity<ApiResponse<PageResponse<AdminCustomerBookingSummaryResponse>>> customerBookings(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminUserService.listCustomerBookings(id, page, size)));
    }

    /** FR-09 — PATCH Customer ACTIVE ↔ SUSPENDED. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateCustomerStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerStatusRequest req,
            @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Cập nhật trạng thái thành công",
                adminUserService.updateCustomerStatus(id, req, actor)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateUserRequest req,
            @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Cập nhật user thành công",
                adminUserService.updateUser(id, req, actor)));
    }
}
