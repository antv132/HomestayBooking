package com.homestay.dto.response;

import com.homestay.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO response cho SCR-50/51 - Manager/Customer Directory (Admin).
 * Endpoint: GET /api/admin/users
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {

    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String status;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** SCR-50 — Số property ACTIVE được gán (null nếu không phải Manager). */
    private Integer propertiesAssigned;

    /** SCR-51 — Tổng đơn đặt phòng (null nếu không phải Customer). */
    private Long totalBookings;

    /** SCR-51 — Tổng chi tiêu PAID (null nếu không phải Customer). */
    private java.math.BigDecimal totalSpend;

    public static AdminUserResponse fromEntity(User u) {
        return fromEntity(u, null, null, null);
    }

    public static AdminUserResponse fromEntity(User u, Integer propertiesAssigned) {
        return fromEntity(u, propertiesAssigned, null, null);
    }

    public static AdminUserResponse fromEntity(
            User u, Integer propertiesAssigned, Long totalBookings, java.math.BigDecimal totalSpend) {
        return AdminUserResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .role(u.getRole().name())
                .status(u.getStatus().name())
                .avatarUrl(u.getAvatarUrl())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .propertiesAssigned(propertiesAssigned)
                .totalBookings(totalBookings)
                .totalSpend(totalSpend)
                .build();
    }
}