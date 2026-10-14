package com.homestay.dto.response;

import com.homestay.entity.Property;
import com.homestay.entity.Room;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

// Response cho SCR-33 (Property List) và SCR-34 (Property Detail)
@Data
public class PropertyResponse {

    private UUID id;
    private String name;
    private String address;
    private String description;
    private String status;
    private int totalFloors;
    private int totalRooms;
    private int availableRooms;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PropertyResponse fromEntity(Property property) {
        PropertyResponse res = new PropertyResponse();
        res.setId(property.getId());
        res.setName(property.getName());
        res.setAddress(property.getAddress());
        res.setDescription(property.getDescription());
        res.setStatus(property.getStatus().name());
        res.setTotalFloors(property.getFloors() != null ? property.getFloors().size() : 0);
        res.setTotalRooms(property.getRooms() != null ? property.getRooms().size() : 0);
        res.setAvailableRooms(property.getRooms() != null
                ? (int) property.getRooms().stream()
                    .filter(r -> r.getStatus() == Room.Status.AVAILABLE)
                    .count()
                : 0);
        res.setCreatedAt(property.getCreatedAt());
        res.setUpdatedAt(property.getUpdatedAt());
        return res;
    }
}
