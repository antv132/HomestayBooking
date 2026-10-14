package com.homestay.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ComplaintDetailResponse {
    private UUID id;
    private String subject;
    private String description;
    private String status;
    private String resolutionNotes;
    private LocalDateTime resolvedAt;
    private CustomerInfo customer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class CustomerInfo {
        private UUID id;
        private String fullName;
        private String email;
    }
}
