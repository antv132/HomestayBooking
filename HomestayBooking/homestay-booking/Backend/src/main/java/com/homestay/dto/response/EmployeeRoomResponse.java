package com.homestay.dto.response;

import com.homestay.entity.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** SCR-65 - Property Room List response for Employee. Read-only reference view. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRoomResponse {

    private UUID id;
    private String roomNumber;
    private String name;
    private String floorName;
    private String status;

    public static EmployeeRoomResponse fromEntity(Room r) {
        String floorName = null;
        if (r.getFloor() != null && r.getFloor().getFloorNumber() != null) {
            floorName = "Tang " + r.getFloor().getFloorNumber();
        }
        return EmployeeRoomResponse.builder()
                .id(r.getId())
                .roomNumber(r.getRoomNumber())
                .name(r.getRoomNumber())
                .floorName(floorName)
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .build();
    }
}