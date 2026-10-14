package com.homestay.dto.response;

import com.homestay.entity.RoomInspection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/** SCR-42: Tóm tắt kiểm tra phòng cho Manager (dùng cả list + drawer log). Read-only. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InspectionSummaryResponse {

    private UUID id;
    private UUID roomId;
    private String roomNumber;
    private UUID propertyId;
    private String propertyName;
    private UUID bookingId;
    private UUID inspectorId;
    private String inspectorName;
    private String status;
    private String note;
    private LocalDateTime inspectedAt;
    private LocalDateTime createdAt;
    private Long inspectionDurationMinutes;

    public static InspectionSummaryResponse fromEntity(RoomInspection ri) {
        Long durationMinutes = null;
        if (ri.getInspectedAt() != null && ri.getCreatedAt() != null) {
            durationMinutes = Duration.between(ri.getCreatedAt(), ri.getInspectedAt()).toMinutes();
        }
        return new InspectionSummaryResponse(
                ri.getId(),
                ri.getRoom().getId(),
                ri.getRoom().getRoomNumber(),
                ri.getProperty().getId(),
                ri.getProperty().getName(),
                ri.getBooking().getId(),
                ri.getInspectedBy() != null ? ri.getInspectedBy().getId() : null,
                ri.getInspectedBy() != null ? ri.getInspectedBy().getFullName() : null,
                ri.getStatus().name(),
                ri.getNote(),
                ri.getInspectedAt(),
                ri.getCreatedAt(),
                durationMinutes
        );
    }
}
