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
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomControllerUnitTest {

    @Mock RoomService roomService;

    private RoomController controller;
    private User manager;

    @BeforeEach
    void setUp() {
        controller = new RoomController(roomService);
        manager = TestFixtures.user(User.Role.MANAGER);
    }

    @Test
    void createUpdateDeleteStatusManagerListUpload() {
        when(roomService.create(any())).thenReturn(new RoomDetailResponse());
        assertEquals(HttpStatus.CREATED, controller.create(new CreateRoomRequest()).getStatusCode());

        UUID id = UUID.randomUUID();
        when(roomService.update(eq(id), any())).thenReturn(new RoomDetailResponse());
        controller.update(id, new UpdateRoomRequest());

        when(roomService.updateStatusForManager(eq(manager), eq(id), any()))
                .thenReturn(new RoomDetailResponse());
        controller.updateStatus(id, new UpdateRoomStatusRequest(), manager);

        when(roomService.getAllForManager(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));
        controller.getAllForManager(null, null, null, null, null, 0, 20, "createdAt,desc");

        controller.deleteRoom(id);
        verify(roomService).deleteRoom(id);

        when(roomService.uploadImages(eq(id), any(), eq(true))).thenReturn(List.of());
        assertEquals(HttpStatus.CREATED,
                controller.uploadImages(id, List.of(new MockMultipartFile("f", "a.jpg", "image/jpeg", new byte[]{1})), true)
                        .getStatusCode());
    }

    @Test
    void getReviewsAndBookings() {
        UUID id = UUID.randomUUID();
        when(roomService.getPublishedReviews(eq(id), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 5, 0, 0));
        controller.getReviews(id, 0, 5);

        when(roomService.getRoomBookings(eq(id), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0));
        controller.getRoomBookings(id, 0, 10, "createdAt,desc");
    }
}
