package com.homestay.dto.response;

import com.homestay.entity.Floor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class FloorResponse {

    private UUID id;
    private UUID propertyId;
    private Integer floorNumber;
    private String description;
    private int roomCount;          // alias: was totalRooms, renamed to match frontend spec
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FloorResponse fromEntity(Floor floor) {
        FloorResponse res = new FloorResponse();
        res.setId(floor.getId());
        res.setPropertyId(floor.getProperty().getId());
        res.setFloorNumber(floor.getFloorNumber());
        res.setDescription(floor.getDescription());
        res.setRoomCount(floor.getRooms() != null ? floor.getRooms().size() : 0);
        res.setCreatedAt(floor.getCreatedAt());
        res.setUpdatedAt(floor.getUpdatedAt());
        return res;
    }
}
