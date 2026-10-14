package com.homestay.dto.response;

import com.homestay.entity.DamageReport;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** SCR-43: Tóm tắt báo cáo hư hại cho Manager (danh sách). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DamageReportSummaryResponse {

    private UUID id;
    private UUID bookingId;
    private String roomNumber;
    private UUID propertyId;
    private String propertyName;
    private BigDecimal totalEstimatedCost;
    private BigDecimal approvedAmount;
    private String status;
    private String inspectorName;
    private String approvedByName;
    private Boolean requiresAdminEscalation;
    private String note;
    private LocalDateTime createdAt;

    public static DamageReportSummaryResponse fromEntity(DamageReport dr) {
        return new DamageReportSummaryResponse(
                dr.getId(),
                dr.getBooking().getId(),
                dr.getInspection().getRoom().getRoomNumber(),
                dr.getInspection().getProperty().getId(),
                dr.getInspection().getProperty().getName(),
                dr.getTotalEstimatedCost(),
                dr.getApprovedAmount(),
                dr.getStatus().name(),
                dr.getInspection().getInspectedBy() != null
                        ? dr.getInspection().getInspectedBy().getFullName() : null,
                dr.getApprovedBy() != null ? dr.getApprovedBy().getFullName() : null,
                dr.getRequiresAdminEscalation(),
                dr.getNote(),
                dr.getCreatedAt()
        );
    }
}
