package com.homestay.dto.response;

import com.homestay.entity.Property;
import com.homestay.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO response cho SCR-46 — Property Management (Admin list).
 * Endpoint: GET /api/admin/properties
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPropertyResponse {

    private UUID id;
    private String name;
    /** Địa điểm — map từ property.address */
    private String location;
    private String description;
    private String status;
    /** Manager ACTIVE đang quản lý (null nếu chưa gán) */
    private UUID managerId;
    private String managerName;
    private LocalDateTime createdAt;

    /**
     * @param manager Manager ACTIVE của property (có thể null nếu chưa gán).
     */
    public static AdminPropertyResponse fromEntity(Property property, User manager) {
        return AdminPropertyResponse.builder()
                .id(property.getId())
                .name(property.getName())
                .location(property.getAddress())
                .description(property.getDescription())
                .status(property.getStatus().name())
                .managerId(manager != null ? manager.getId() : null)
                .managerName(manager != null ? manager.getFullName() : null)
                .createdAt(property.getCreatedAt())
                .build();
    }
}
