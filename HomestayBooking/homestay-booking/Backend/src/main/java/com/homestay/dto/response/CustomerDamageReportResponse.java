package com.homestay.dto.response;

import com.homestay.entity.DamageReport;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

/** Customer-visible damage report with dispute eligibility (023 US6). */
@Data
@Builder
public class CustomerDamageReportResponse {

    public static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final int DISPUTE_WINDOW_HOURS = 24;

    private UUID id;
    private UUID bookingId;
    private String roomNumber;
    private String propertyName;
    private String status;
    private BigDecimal totalEstimatedCost;
    private BigDecimal approvedAmount;
    private LocalDateTime approvedAt;
    private LocalDateTime disputeDeadline;
    private boolean disputeEligible;
    private String disputeNote;
    private LocalDateTime disputedAt;
    private String note;
    private LocalDateTime createdAt;

    public static CustomerDamageReportResponse fromEntity(DamageReport dr) {
        LocalDateTime deadline = disputeDeadlineOf(dr.getApprovedAt());
        return CustomerDamageReportResponse.builder()
                .id(dr.getId())
                .bookingId(dr.getBooking().getId())
                .roomNumber(dr.getInspection().getRoom().getRoomNumber())
                .propertyName(dr.getInspection().getProperty().getName())
                .status(dr.getStatus().name())
                .totalEstimatedCost(dr.getTotalEstimatedCost())
                .approvedAmount(dr.getApprovedAmount())
                .approvedAt(dr.getApprovedAt())
                .disputeDeadline(deadline)
                .disputeEligible(isDisputeEligible(dr, deadline))
                .disputeNote(dr.getDisputeNote())
                .disputedAt(dr.getDisputedAt())
                .note(dr.getNote())
                .createdAt(dr.getCreatedAt())
                .build();
    }

    public static LocalDateTime disputeDeadlineOf(LocalDateTime approvedAt) {
        if (approvedAt == null) {
            return null;
        }
        return approvedAt.atZone(VN_ZONE).plusHours(DISPUTE_WINDOW_HOURS).toLocalDateTime();
    }

    public static boolean isDisputeEligible(DamageReport dr, LocalDateTime deadline) {
        if (dr.getStatus() != DamageReport.Status.APPROVED || dr.getApprovedAt() == null || deadline == null) {
            return false;
        }
        ZonedDateTime now = ZonedDateTime.now(VN_ZONE);
        return !now.isAfter(deadline.atZone(VN_ZONE));
    }
}
