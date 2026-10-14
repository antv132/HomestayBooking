package com.homestay.dto.response;

import com.homestay.entity.Floor;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyDetailResponseTest {

    @Test
    void fromEntity_withFullyPopulatedProperty_mapsAllFields() {
        Property property = createPropertyWithRooms();

        PropertyDetailResponse response = PropertyDetailResponse.fromEntity(property);

        assertThat(response.getId()).isEqualTo(property.getId());
        assertThat(response.getName()).isEqualTo("Test Property");
        assertThat(response.getAddress()).isEqualTo("123 Test St");
        assertThat(response.getDescription()).isEqualTo("Test Description");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void fromEntity_calculatesStatsCorrectly() {
        Property property = createPropertyWithRooms();

        PropertyDetailResponse response = PropertyDetailResponse.fromEntity(property);

        assertThat(response.getStats()).isNotNull();
        assertThat(response.getStats().getTotalFloors()).isEqualTo(2);
        assertThat(response.getStats().getTotalRooms()).isEqualTo(5);
        assertThat(response.getStats().getAvailableRooms()).isEqualTo(2);
        assertThat(response.getStats().getReservedRooms()).isEqualTo(1);
        assertThat(response.getStats().getOccupiedRooms()).isEqualTo(1);
        assertThat(response.getStats().getMaintenanceRooms()).isEqualTo(1);
        assertThat(response.getStats().getPendingDepositRooms()).isEqualTo(0);
    }

    @Test
    void fromEntity_mapsFloorSummariesWithRoomCounts() {
        Property property = createPropertyWithRooms();

        PropertyDetailResponse response = PropertyDetailResponse.fromEntity(property);

        assertThat(response.getFloors()).hasSize(2);

        PropertyDetailResponse.FloorSummary floor1 = response.getFloors().get(0);
        assertThat(floor1.getFloorNumber()).isEqualTo(1);
        assertThat(floor1.getRoomCount()).isEqualTo(3);
        assertThat(floor1.getAvailableCount()).isEqualTo(2);

        PropertyDetailResponse.FloorSummary floor2 = response.getFloors().get(1);
        assertThat(floor2.getFloorNumber()).isEqualTo(2);
        assertThat(floor2.getRoomCount()).isEqualTo(2);
        assertThat(floor2.getAvailableCount()).isEqualTo(0);
    }

    @Test
    void fromEntity_filtersDeletedRooms() {
        Property property = createPropertyWithRooms();
        Room deletedRoom = property.getFloors().get(0).getRooms().get(0);
        deletedRoom.setDeletedAt(LocalDateTime.now());

        PropertyDetailResponse response = PropertyDetailResponse.fromEntity(property);

        assertThat(response.getStats().getTotalRooms()).isEqualTo(4);
        assertThat(response.getFloors().get(0).getRoomCount()).isEqualTo(2);
    }

    @Test
    void fromEntity_handlesEmptyFloorsAndRooms() {
        Property property = new Property();
        property.setId(UUID.randomUUID());
        property.setName("Empty Property");
        property.setAddress("Address");
        property.setDescription("Desc");
        property.setStatus(Property.Status.ACTIVE);
        property.setCreatedAt(LocalDateTime.now());
        property.setUpdatedAt(LocalDateTime.now());
        property.setFloors(new ArrayList<>());

        PropertyDetailResponse response = PropertyDetailResponse.fromEntity(property);

        assertThat(response.getStats().getTotalFloors()).isEqualTo(0);
        assertThat(response.getStats().getTotalRooms()).isEqualTo(0);
        assertThat(response.getFloors()).isEmpty();
    }

    @Test
    void fromEntity_handlesNullFloorsCollection() {
        Property property = new Property();
        property.setId(UUID.randomUUID());
        property.setName("Property");
        property.setAddress("Address");
        property.setDescription("Desc");
        property.setStatus(Property.Status.ACTIVE);
        property.setCreatedAt(LocalDateTime.now());
        property.setUpdatedAt(LocalDateTime.now());
        property.setFloors(null);

        PropertyDetailResponse response = PropertyDetailResponse.fromEntity(property);

        assertThat(response.getStats().getTotalFloors()).isEqualTo(0);
        assertThat(response.getStats().getTotalRooms()).isEqualTo(0);
        assertThat(response.getFloors()).isEmpty();
    }

    @Test
    void fromEntity_sortsFloorsByFloorNumber() {
        Property property = new Property();
        property.setId(UUID.randomUUID());
        property.setName("Property");
        property.setAddress("Address");
        property.setDescription("Desc");
        property.setStatus(Property.Status.ACTIVE);
        property.setCreatedAt(LocalDateTime.now());
        property.setUpdatedAt(LocalDateTime.now());

        List<Floor> floors = new ArrayList<>();
        floors.add(createFloor(property, 3, 0));
        floors.add(createFloor(property, 1, 0));
        floors.add(createFloor(property, 2, 0));
        property.setFloors(floors);

        PropertyDetailResponse response = PropertyDetailResponse.fromEntity(property);

        assertThat(response.getFloors()).hasSize(3);
        assertThat(response.getFloors().get(0).getFloorNumber()).isEqualTo(1);
        assertThat(response.getFloors().get(1).getFloorNumber()).isEqualTo(2);
        assertThat(response.getFloors().get(2).getFloorNumber()).isEqualTo(3);
    }

    private Property createPropertyWithRooms() {
        Property property = new Property();
        property.setId(UUID.randomUUID());
        property.setName("Test Property");
        property.setAddress("123 Test St");
        property.setDescription("Test Description");
        property.setStatus(Property.Status.ACTIVE);
        property.setCreatedAt(LocalDateTime.now());
        property.setUpdatedAt(LocalDateTime.now());

        List<Floor> floors = new ArrayList<>();
        Floor floor1 = createFloor(property, 1, 3);
        Floor floor2 = createFloor(property, 2, 2);
        floors.add(floor1);
        floors.add(floor2);
        property.setFloors(floors);

        List<Room> floor1Rooms = floor1.getRooms();
        floor1Rooms.get(0).setStatus(Room.Status.AVAILABLE);
        floor1Rooms.get(1).setStatus(Room.Status.AVAILABLE);
        floor1Rooms.get(2).setStatus(Room.Status.RESERVED);

        List<Room> floor2Rooms = floor2.getRooms();
        floor2Rooms.get(0).setStatus(Room.Status.OCCUPIED);
        floor2Rooms.get(1).setStatus(Room.Status.MAINTENANCE);

        return property;
    }

    private Floor createFloor(Property property, int floorNumber, int roomCount) {
        Floor floor = new Floor();
        floor.setId(UUID.randomUUID());
        floor.setProperty(property);
        floor.setFloorNumber(floorNumber);
        floor.setDescription("Floor " + floorNumber);

        List<Room> rooms = new ArrayList<>();
        for (int i = 0; i < roomCount; i++) {
            Room room = new Room();
            room.setId(UUID.randomUUID());
            room.setProperty(property);
            room.setFloor(floor);
            room.setRoomNumber(floorNumber + "0" + (i + 1));
            room.setRoomType("DELUXE");
            room.setStatus(Room.Status.AVAILABLE);
            rooms.add(room);
        }
        floor.setRooms(rooms);

        return floor;
    }
}
