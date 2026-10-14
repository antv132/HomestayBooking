package com.homestay.dto.response;

import com.homestay.entity.RoomInspection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** SCR-64 - Room/inspection eligible for a new damage report. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeEligibleDamageRoomResponse {

    private String roomId;
    private String roomNumber;
    private String inspectionId;
    private LocalDateTime inspectedAt;

    public static EmployeeEligibleDamageRoomResponse fromEntity(RoomInspection ri) {
        String roomNumber = ri.getRoom() != null ? ri.getRoom().getRoomNumber() : null;
        return EmployeeEligibleDamageRoomResponse.builder()
                .roomId(ri.getRoom() != null ? ri.getRoom().getId().toString() : null)
                .roomNumber(roomNumber)
                .inspectionId(ri.getId().toString())
                .inspectedAt(ri.getInspectedAt() != null ? ri.getInspectedAt() : ri.getCreatedAt())
                .build();
    }
}