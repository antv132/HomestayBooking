package com.homestay.dto.response;

import com.homestay.entity.Attachment;
import com.homestay.entity.DamageReport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SCR-53 - Damage Escalation (Admin). Field khop FE AdminDamageReport.
 * status/attachments duoc suy dien; entity khong luu truc tiep.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDamageReportResponse {

    private String id;
    private String roomId;
    private String roomName;
    private String propertyName;
    private String reportedBy;
    private BigDecimal totalFee;
    private String status;
    private List<Item> items;
    private List<AttachmentDto> attachments;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String name;
        private BigDecimal estimatedCost;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentDto {
        private String url;
        private String type;
    }

    public static AdminDamageReportResponse from(DamageReport dr, List<Attachment> reportAttachments) {
        var inspection = dr.getInspection();
        String reportedBy = (inspection.getInspectedBy() != null)
                ? inspection.getInspectedBy().getFullName() : "";

        List<Item> items = (dr.getItems() == null) ? List.of()
                : dr.getItems().stream()
                    .map(it -> Item.builder()
                            .name(it.getItemName())
                            .estimatedCost(it.getEstimatedCost())
                            .build())
                    .toList();

        List<AttachmentDto> atts = (reportAttachments == null) ? List.of()
                : reportAttachments.stream()
                    .map(a -> AttachmentDto.builder()
                            .url(a.getFileUrl())
                            .type("IMAGE")
                            .build())
                    .toList();

        return AdminDamageReportResponse.builder()
                .id(dr.getId().toString())
                .roomId(inspection.getRoom().getId().toString())
                .roomName(inspection.getRoom().getRoomNumber())
                .propertyName(inspection.getProperty().getName())
                .reportedBy(reportedBy)
                .totalFee(dr.getTotalEstimatedCost())
                .status(mapStatus(dr.getStatus()))
                .items(items)
                .attachments(atts)
                .createdAt(dr.getCreatedAt())
                .build();
    }

    // Map entity status -> FE enum ('ESCALATED' | 'APPROVED' | 'PENDING_REVIEW').
    private static String mapStatus(DamageReport.Status status) {
        if (status == DamageReport.Status.APPROVED) {
            return "APPROVED";
        }
        if (status == DamageReport.Status.PENDING_APPROVAL) {
            return "ESCALATED";
        }
        return "PENDING_REVIEW";
    }
}