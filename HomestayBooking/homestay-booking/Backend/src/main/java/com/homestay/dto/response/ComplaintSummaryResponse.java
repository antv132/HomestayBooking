package com.homestay.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ComplaintSummaryResponse {
    private UUID id;
    private String subject;
    private String customerName;
    private String status;
    private LocalDateTime createdAt;
}
