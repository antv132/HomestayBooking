package com.homestay.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoomCalendarPriorityTest {

    @Test
    void occupiedBeatsMaintenance() {
        assertEquals("OCCUPIED", RoomService.higherPriorityStatus("OCCUPIED", "MAINTENANCE"));
        assertEquals("OCCUPIED", RoomService.higherPriorityStatus("MAINTENANCE", "OCCUPIED"));
    }

    @Test
    void pendingDepositBeatsReserved() {
        assertEquals("PENDING_DEPOSIT", RoomService.higherPriorityStatus("PENDING_DEPOSIT", "RESERVED"));
    }

    @Test
    void availableIsLowest() {
        assertEquals("MAINTENANCE", RoomService.higherPriorityStatus("AVAILABLE", "MAINTENANCE"));
        assertEquals("OUT_OF_SERVICE", RoomService.higherPriorityStatus("AVAILABLE", "OUT_OF_SERVICE"));
    }
}
