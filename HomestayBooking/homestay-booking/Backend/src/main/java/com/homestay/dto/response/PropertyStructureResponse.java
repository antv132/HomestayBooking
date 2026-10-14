package com.homestay.dto.response;

import com.homestay.entity.Floor;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import lombok.Data;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SCR-37 — Property Structure Tree response.
 * Returns the full Property → Floor(s) → Room(s) hierarchy.
 */
@Data
public class PropertyStructureResponse {

    private UUID propertyId;
    private String propertyName;
    private List<FloorNode> floors;

    // ── Inner DTOs ────────────────────────────────────────────────────────────

    @Data
    public static class RoomNode {
        private UUID id;
        private String roomNumber;
        private String roomType;
        private String status;
        private Double pricePerNight;
        private Integer capacity;
    }

    @Data
    public static class FloorNode {
        private UUID id;
        private Integer floorNumber;
        private String description;
        private Integer roomCount;
        private List<RoomNode> rooms;
    }

    // ── Static factory ────────────────────────────────────────────────────────

    public static PropertyStructureResponse fromEntity(Property property) {
        PropertyStructureResponse res = new PropertyStructureResponse();
        res.setPropertyId(property.getId());
        res.setPropertyName(property.getName());

        List<FloorNode> floorNodes = (property.getFloors() == null)
                ? List.of()
                : property.getFloors().stream()
                    .sorted((a, b) -> Integer.compare(a.getFloorNumber(), b.getFloorNumber()))
                    .map(PropertyStructureResponse::toFloorNode)
                    .collect(Collectors.toList());

        res.setFloors(floorNodes);
        return res;
    }

    private static FloorNode toFloorNode(Floor floor) {
        FloorNode node = new FloorNode();
        node.setId(floor.getId());
        node.setFloorNumber(floor.getFloorNumber());
        node.setDescription(floor.getDescription());

        List<RoomNode> rooms = (floor.getRooms() == null)
                ? List.of()
                : floor.getRooms().stream()
                    .map(PropertyStructureResponse::toRoomNode)
                    .collect(Collectors.toList());

        node.setRooms(rooms);
        node.setRoomCount(rooms.size());
        return node;
    }

    private static RoomNode toRoomNode(Room room) {
        RoomNode node = new RoomNode();
        node.setId(room.getId());
        node.setRoomNumber(room.getRoomNumber());
        node.setRoomType(room.getRoomType());
        node.setStatus(room.getStatus() != null ? room.getStatus().name() : null);
        node.setPricePerNight(room.getPricePerNight() != null
                ? room.getPricePerNight().doubleValue() : null);
        node.setCapacity(room.getCapacity());
        return node;
    }
}
