package com.homestay.controller;

import com.homestay.dto.request.CreateRoomRequest;
import com.homestay.dto.request.UpdateRoomRequest;
import com.homestay.dto.request.UpdateRoomStatusRequest;
import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.RoomDetailResponse;
import com.homestay.entity.User;
import com.homestay.service.RoomService;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomV1ControllerTest {

    @Mock RoomService roomService;

    private RoomV1Controller controller;
    private User manager;

    @BeforeEach
    void setUp() {
        controller = new RoomV1Controller(roomService);
        manager = TestFixtures.user(User.Role.MANAGER);
    }

    @Test
    void listRooms() {
        when(roomService.getAllForManagerScoped(eq(manager), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0));
        assertEquals(HttpStatus.OK,
                controller.listRooms(null, null, null, null, null, 0, 10, manager).getStatusCode());
    }

    @Test
    void createUpdateGetDeleteStatus() {
        CreateRoomRequest create = new CreateRoomRequest();
        when(roomService.createForManager(manager, create)).thenReturn(new RoomDetailResponse());
        assertEquals(HttpStatus.CREATED, controller.create(create, manager).getStatusCode());

        UUID id = UUID.randomUUID();
        when(roomService.getByIdForManager(manager, id)).thenReturn(new RoomDetailResponse());
        controller.getById(id, manager);
        verify(roomService).getByIdForManager(manager, id);

        UpdateRoomRequest update = new UpdateRoomRequest();
        when(roomService.updateForManager(manager, id, update)).thenReturn(new RoomDetailResponse());
        controller.update(id, update, manager);

        UpdateRoomStatusRequest status = new UpdateRoomStatusRequest();
        when(roomService.updateStatusForManager(manager, id, status)).thenReturn(new RoomDetailResponse());
        controller.updateStatus(id, status, manager);

        controller.delete(id, manager);
        verify(roomService).deleteRoomForManager(manager, id);
    }

    @Test
    void calendarAndBlocks() {
        UUID id = UUID.randomUUID();
        when(roomService.listStatusBlocksForManager(manager, id)).thenReturn(List.of());
        controller.listStatusBlocks(id, manager);
        verify(roomService).listStatusBlocksForManager(manager, id);

        when(roomService.getMonthAvailabilityForManager(eq(manager), eq(id), any(), any()))
                .thenReturn(new com.homestay.dto.response.StaffRoomMonthCalendarResponse());
        controller.getCalendar(id, 7, 2026, null, null, manager);
        verify(roomService).getMonthAvailabilityForManager(eq(manager), eq(id), any(LocalDate.class), any(LocalDate.class));
    }
}
