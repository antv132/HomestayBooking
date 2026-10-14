package com.homestay.dto.response;

import com.homestay.entity.Floor;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyStructureResponseTest {

    @Test
    void fromEntity_buildsTreeSortedByFloor() {
        Property property = TestFixtures.property();
        Floor f2 = TestFixtures.floor(property);
        f2.setFloorNumber(2);
        Floor f1 = TestFixtures.floor(property);
        f1.setFloorNumber(1);

        Room r1 = TestFixtures.room(property);
        r1.setFloor(f1);
        r1.setRoomNumber("101");
        r1.setStatus(Room.Status.AVAILABLE);
        r1.setPricePerNight(new BigDecimal("500000"));
        r1.setCapacity(2);

        f1.setRooms(new ArrayList<>(List.of(r1)));
        f2.setRooms(new ArrayList<>());
        property.setFloors(new ArrayList<>(List.of(f2, f1)));

        PropertyStructureResponse res = PropertyStructureResponse.fromEntity(property);

        assertEquals(property.getId(), res.getPropertyId());
        assertEquals(2, res.getFloors().size());
        assertEquals(1, res.getFloors().get(0).getFloorNumber());
        assertEquals(1, res.getFloors().get(0).getRoomCount());
        assertEquals("101", res.getFloors().get(0).getRooms().get(0).getRoomNumber());
    }

    @Test
    void fromEntity_nullFloors() {
        Property property = TestFixtures.property();
        property.setFloors(null);
        PropertyStructureResponse res = PropertyStructureResponse.fromEntity(property);
        assertTrue(res.getFloors().isEmpty());
    }
}
