package com.homestay.dto.response;

import com.homestay.entity.HousekeepingTask;
import com.homestay.entity.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** SCR-60 - Employee housekeeping task list item. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeHousekeepingTaskResponse {

    private String id;
    private String roomId;
    private String roomNumber;
    private String floorName;
    private String status;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public static EmployeeHousekeepingTaskResponse fromEntity(HousekeepingTask task) {
        Room room = task.getRoom();
        String roomId = null;
        String roomNumber = null;
        String floorName = null;
        if (room != null) {
            roomId = room.getId() != null ? room.getId().toString() : null;
            roomNumber = room.getRoomNumber();
            if (room.getFloor() != null && room.getFloor().getFloorNumber() != null) {
                floorName = "Tang " + room.getFloor().getFloorNumber();
            }
        }
        return EmployeeHousekeepingTaskResponse.builder()
                .id(task.getId().toString())
                .roomId(roomId)
                .roomNumber(roomNumber)
                .floorName(floorName)
                .status(task.getStatus().name())
                .note(task.getNote())
                .createdAt(task.getCreatedAt())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }
}
