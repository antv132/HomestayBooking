package com.homestay.dto.response;

import com.homestay.entity.ActivityLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogEntryResponse {

    private UUID id;
    private UUID userId;
    private String userEmail;
    private String userFullName;
    private String action;
    private String entityType;
    private UUID entityId;
    private String description;
    private LocalDateTime createdAt;

    public static ActivityLogEntryResponse from(ActivityLog log) {
        return ActivityLogEntryResponse.builder()
                .id(log.getId())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .userEmail(log.getUser() != null ? log.getUser().getEmail() : null)
                .userFullName(log.getUser() != null ? log.getUser().getFullName() : "System")
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .description(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
