package com.homestay.dto.response;

import com.homestay.entity.RoomInspection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** SCR-62 - Employee room inspection list item. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeInspectionResponse {

    private String id;
    private RoomRef room;
    private String bookingId;
    private String status;
    private LocalDateTime inspectedAt;

    /** Inspector currently assigned / claimed; null = Unassigned (Claim). */
    private String inspectorId;
    private String inspectorName;

    /** Convenience fields kept for existing employee UI consumers. */
    private String roomId;
    private String roomNumber;
    private String roomName;
    private LocalDateTime createdAt;
    private String note;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomRef {
        private String id;
        private String roomNumber;
    }

    public static EmployeeInspectionResponse fromEntity(RoomInspection ri) {
        String roomId = ri.getRoom() != null ? ri.getRoom().getId().toString() : null;
        String roomNumber = ri.getRoom() != null ? ri.getRoom().getRoomNumber() : null;
        String inspectorId = ri.getInspectedBy() != null ? ri.getInspectedBy().getId().toString() : null;
        String inspectorName = ri.getInspectedBy() != null ? ri.getInspectedBy().getFullName() : null;
        return EmployeeInspectionResponse.builder()
                .id(ri.getId().toString())
                .room(RoomRef.builder().id(roomId).roomNumber(roomNumber).build())
                .bookingId(ri.getBooking() != null ? ri.getBooking().getId().toString() : null)
                .status(ri.getStatus().name())
                .inspectedAt(ri.getInspectedAt())
                .inspectorId(inspectorId)
                .inspectorName(inspectorName)
                .roomId(roomId)
                .roomNumber(roomNumber)
                .roomName(roomNumber)
                .createdAt(ri.getCreatedAt())
                .note(ri.getNote())
                .build();
    }
}
