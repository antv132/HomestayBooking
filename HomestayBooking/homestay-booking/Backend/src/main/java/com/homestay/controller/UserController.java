package com.homestay.controller;

import com.homestay.dto.request.ChangePasswordRequest;
import com.homestay.dto.request.UpdateProfileRequest;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.UserProfileResponse;
import com.homestay.entity.User;
import com.homestay.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Lấy thông tin profile của mình
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal User currentUser) {

        UserProfileResponse profile = userService.getMyProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    // Cập nhật profile của mình
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {

        UserProfileResponse profile = userService.updateMyProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thông tin thành công", profile));
    }

    // Đổi mật khẩu
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Đổi mật khẩu thành công. Vui lòng đăng nhập lại."));
    }

    // --- MANAGER API ---
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<com.homestay.dto.response.PageResponse<com.homestay.dto.response.CustomerSummaryResponse>>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAllCustomers(page, size, status, search)));
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/customers/{id}")
    public ResponseEntity<ApiResponse<com.homestay.dto.response.CustomerDetailResponse>> getCustomerDetail(@PathVariable java.util.UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getCustomerDetail(id)));
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('MANAGER')")
    @PatchMapping("/customers/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateCustomerStatus(
            @PathVariable java.util.UUID id,
            @RequestBody java.util.Map<String, String> body
    ) {
        userService.updateCustomerStatus(id, body.get("status"));
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành công"));
    }
}
