package com.homestay.dto.response;

import com.homestay.entity.Complaint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SCR-54 - Complaint Management (Admin).
 * Khớp cột: Ticket ID, Customer, Subject, Status, Submitted Date.
 * Entity không có booking — không trả bookingId giả.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminComplaintResponse {

    private String id;
    private String customerId;
    private String customerName;
    private String subject;
    private String description;
    private String status;
    private String resolution;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public static AdminComplaintResponse from(Complaint c) {
        String customerId = null;
        String customerName = "Khách vãng lai";
        if (c.getUser() != null) {
            customerId = c.getUser().getId().toString();
            customerName = c.getUser().getFullName();
        }
        return AdminComplaintResponse.builder()
                .id(c.getId().toString())
                .customerId(customerId)
                .customerName(customerName)
                .subject(c.getSubject())
                .description(c.getDescription())
                .status(c.getStatus().name())
                .resolution(c.getResolutionNotes())
                .createdAt(c.getCreatedAt())
                .resolvedAt(c.getResolvedAt())
                .build();
    }
}
