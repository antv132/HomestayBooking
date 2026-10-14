package com.homestay.dto.response;

import com.homestay.entity.Floor;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Full property detail response — SCR-34
 * Trả về stats đầy đủ + danh sách floors với roomCount từng tầng.
 */
@Data
public class PropertyDetailResponse {

    private UUID id;
    private String name;
    private String address;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Stats stats;
    private List<FloorSummary> floors;

    // ── Inner classes ───────────────────────────────────────────────────────────

    @Data
    public static class Stats {
        private int totalFloors;
        private int totalRooms;
        private int availableRooms;
        private int pendingDepositRooms;
        private int reservedRooms;
        private int occupiedRooms;
        private int maintenanceRooms;
    }

    @Data
    public static class FloorSummary {
        private UUID id;
        private int floorNumber;
        private String description;
        private int roomCount;
        private int availableCount;
    }

    // ── Factory ─────────────────────────────────────────────────────────────────

    public static PropertyDetailResponse fromEntity(Property property) {
        PropertyDetailResponse res = new PropertyDetailResponse();
        res.setId(property.getId());
        res.setName(property.getName());
        res.setAddress(property.getAddress());
        res.setDescription(property.getDescription());
        res.setStatus(property.getStatus().name());
        res.setCreatedAt(property.getCreatedAt());
        res.setUpdatedAt(property.getUpdatedAt());

        // Stats from floor.rooms (two-step load — dual JOIN FETCH floors+rooms → MultipleBagFetchException)
        List<Floor> floors = property.getFloors() != null ? property.getFloors() : List.of();
        List<Room> rooms = floors.stream()
                .flatMap(f -> (f.getRooms() != null ? f.getRooms() : List.<Room>of()).stream())
                .filter(r -> r.getDeletedAt() == null)
                .collect(Collectors.toList());

        Stats stats = new Stats();
        stats.setTotalFloors(floors.size());
        stats.setTotalRooms(rooms.size());
        stats.setAvailableRooms(countByStatus(rooms, Room.Status.AVAILABLE));
        stats.setPendingDepositRooms(countByStatus(rooms, Room.Status.PENDING_DEPOSIT));
        stats.setReservedRooms(countByStatus(rooms, Room.Status.RESERVED));
        stats.setOccupiedRooms(countByStatus(rooms, Room.Status.OCCUPIED));
        stats.setMaintenanceRooms(countByStatus(rooms, Room.Status.MAINTENANCE));
        res.setStats(stats);

        List<FloorSummary> floorSummaries = floors.stream()
                .sorted((a, b) -> Integer.compare(a.getFloorNumber(), b.getFloorNumber()))
                .map(f -> {
                    FloorSummary fs = new FloorSummary();
                    fs.setId(f.getId());
                    fs.setFloorNumber(f.getFloorNumber());
                    fs.setDescription(f.getDescription());
                    List<Room> floorRooms = f.getRooms() != null
                            ? f.getRooms().stream().filter(r -> r.getDeletedAt() == null).collect(Collectors.toList())
                            : List.of();
                    fs.setRoomCount(floorRooms.size());
                    fs.setAvailableCount(countByStatus(floorRooms, Room.Status.AVAILABLE));
                    return fs;
                })
                .collect(Collectors.toList());
        res.setFloors(floorSummaries);

        return res;
    }

    private static int countByStatus(List<Room> rooms, Room.Status status) {
        return (int) rooms.stream().filter(r -> r.getStatus() == status).count();
    }
}
