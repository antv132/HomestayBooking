package com.homestay.dto.response;

import com.homestay.entity.HousekeepingTask;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EmployeeHousekeepingTaskResponseTest {

    @Test
    void fromEntity_withFloor() {
        Property property = TestFixtures.property();
        Room room = TestFixtures.room(property);
        HousekeepingTask task = new HousekeepingTask();
        task.setId(UUID.randomUUID());
        task.setRoom(room);
        task.setStatus(HousekeepingTask.Status.IN_PROGRESS);
        task.setNote("Clean bathroom");
        task.setCreatedAt(LocalDateTime.now());
        task.setStartedAt(LocalDateTime.now());

        EmployeeHousekeepingTaskResponse res = EmployeeHousekeepingTaskResponse.fromEntity(task);
        assertEquals(task.getId().toString(), res.getId());
        assertEquals(room.getId().toString(), res.getRoomId());
        assertEquals("101", res.getRoomNumber());
        assertEquals("Tang 1", res.getFloorName());
        assertEquals(HousekeepingTask.Status.IN_PROGRESS.name(), res.getStatus());
    }

    @Test
    void fromEntity_nullRoom() {
        HousekeepingTask task = new HousekeepingTask();
        task.setId(UUID.randomUUID());
        task.setStatus(HousekeepingTask.Status.PENDING);
        EmployeeHousekeepingTaskResponse res = EmployeeHousekeepingTaskResponse.fromEntity(task);
        assertNull(res.getRoomId());
        assertNull(res.getFloorName());
    }
}
