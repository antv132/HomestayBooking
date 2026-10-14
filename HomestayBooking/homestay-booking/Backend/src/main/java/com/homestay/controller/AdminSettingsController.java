package com.homestay.controller;

import com.homestay.dto.request.UpdateSystemSettingsRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.SystemSettingsResponse;
import com.homestay.entity.User;
import com.homestay.service.AdminSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/settings", "/api/v1/admin/settings"})
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final AdminSettingsService adminSettingsService;

    @GetMapping
    public ResponseEntity<ApiResponse<SystemSettingsResponse>> get() {
        return ResponseEntity.ok(ApiResponse.ok(adminSettingsService.getSettings()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<SystemSettingsResponse>> update(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateSystemSettingsRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Cập nhật settings thành công",
                adminSettingsService.updateSettings(body, currentUser)));
    }
}
