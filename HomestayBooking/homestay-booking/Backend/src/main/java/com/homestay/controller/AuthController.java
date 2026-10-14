package com.homestay.controller;

import com.homestay.dto.request.*;
import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.AuthResponse;
import com.homestay.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Đăng ký tài khoản
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản."));
    }

    // Xác thực OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.ok("Xác thực email thành công. Bạn có thể đăng nhập ngay."));
    }

    // Gửi lại OTP
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.ok("Đã gửi lại mã OTP về email của bạn."));
    }

    // Đăng nhập
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Đăng nhập thành công", response));
    }

    // Đăng nhập bằng Google (frontend gửi ID Token lên)
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        AuthResponse response = authService.loginWithGoogle(request);
        String message = response.isPendingVerification()
                ? "Email chưa xác thực. Vui lòng nhập OTP để liên kết Google."
                : "Đăng nhập bằng Google thành công";
        return ResponseEntity.ok(ApiResponse.ok(message, response));
    }

    // FR-006: hoàn tất liên kết Google sau OTP
    @PostMapping("/google/complete-link")
    public ResponseEntity<ApiResponse<AuthResponse>> completeGoogleLink(
            @Valid @RequestBody GoogleCompleteLinkRequest request) {
        AuthResponse response = authService.completeGoogleLink(request);
        return ResponseEntity.ok(ApiResponse.ok("Liên kết Google thành công", response));
    }

    // Quên mật khẩu
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Đã gửi mã đặt lại mật khẩu về email của bạn."));
    }

    // Đặt lại mật khẩu
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Đặt lại mật khẩu thành công. Vui lòng đăng nhập."));
    }

    // Refresh access token
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok("Làm mới token thành công", response));
    }

    // Đăng xuất
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.ok("Đăng xuất thành công."));
    }
}
