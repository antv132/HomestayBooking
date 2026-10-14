package com.homestay.dto.response;

import com.homestay.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

// Response sau khi đăng nhập hoặc refresh token thành công
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private UserInfo user;

    /** FR-006: Google login needs email OTP before linking unverified local account. */
    private boolean pendingVerification;
    private String verificationToken;
    private String email;

    public AuthResponse(String accessToken, String refreshToken, UserInfo user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
        this.pendingVerification = false;
    }

    public static AuthResponse pendingGoogleLink(String verificationToken, String email) {
        AuthResponse response = new AuthResponse();
        response.pendingVerification = true;
        response.verificationToken = verificationToken;
        response.email = email;
        return response;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private UUID id;
        private String fullName;
        private String email;
        private String phone;
        private String avatarUrl;
        private String role;    // "CUSTOMER" hoặc "MANAGER"
        private String status;  // "ACTIVE", "INACTIVE", "SUSPENDED"
    }

    // Helper tạo UserInfo từ entity
    public static UserInfo fromUser(User user) {
        return new UserInfo(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getRole().name(),
                user.getStatus().name()
        );
    }
}
