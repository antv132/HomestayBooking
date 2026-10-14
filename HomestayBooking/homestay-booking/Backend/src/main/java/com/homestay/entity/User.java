package com.homestay.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
public class User {

    // Vai trò trong hệ thống
    public enum Role { ADMIN, MANAGER, EMPLOYEE, CUSTOMER }

    // Trạng thái tài khoản
    public enum Status { INACTIVE, ACTIVE, SUSPENDED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Size(max = 255)
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @NotBlank
    @Email
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    // Mật khẩu đã được mã hóa bằng BCrypt
    // Null nếu user đăng nhập bằng Google (không có password)
    @Column(name = "password_hash", columnDefinition = "TEXT")
    private String passwordHash;

    // Google sub (unique ID từ Google) - null nếu đăng ký bằng email
    @Column(name = "google_id", length = 255)
    private String googleId;

    @Size(max = 20)
    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.CUSTOMER;

    // Mặc định INACTIVE, chờ xác thực email
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.INACTIVE;

    // Mã OTP 6 chữ số gửi qua email
    @Column(name = "otp_code", length = 10)
    private String otpCode;

    @Column(name = "otp_expired_at")
    private LocalDateTime otpExpiredAt;

    /** OTP sends in current 1-hour window (FR-002: max 3/hour/email). */
    @Column(name = "otp_send_count", nullable = false)
    private int otpSendCount = 0;

    @Column(name = "otp_send_window_start")
    private LocalDateTime otpSendWindowStart;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
