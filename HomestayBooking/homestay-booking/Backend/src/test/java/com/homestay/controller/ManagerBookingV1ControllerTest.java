package com.homestay.controller;

import com.homestay.dto.request.CancelBookingRequest;
import com.homestay.dto.request.ManagerCheckInRequest;
import com.homestay.dto.request.ManagerCheckOutRequest;
import com.homestay.dto.request.ModifyBookingRequest;
import com.homestay.dto.response.CancellationPreviewResponse;
import com.homestay.dto.response.ManagerBookingDetailResponse;
import com.homestay.dto.response.ModifyBookingResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.service.BookingService;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerBookingV1ControllerTest {

    @Mock BookingService bookingService;

    private ManagerBookingV1Controller controller;
    private User manager;

    @BeforeEach
    void setUp() {
        controller = new ManagerBookingV1Controller(bookingService);
        manager = TestFixtures.user(User.Role.MANAGER);
    }

    @Test
    void listAndDetailAndPreview() {
                when(bookingService.getBookingsForManagerScoped(
                eq(manager), any(), any(), any(), any(), any(), eq(0), eq(10), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0));
        controller.list(manager, null, 0, 10, null, null, null, null, "checkInDate,desc");

        UUID id = UUID.randomUUID();
        when(bookingService.getBookingDetailForManager(manager, id)).thenReturn(new ManagerBookingDetailResponse());
        controller.getById(id, manager);

        when(bookingService.getManagerCancellationPreview(manager, id))
                .thenReturn(new CancellationPreviewResponse());
        controller.cancelPreview(id, manager);
    }

    @Test
    void cancel_requiresReason() {
        CancelBookingRequest body = new CancelBookingRequest();
        assertThrows(BusinessException.class, () ->
                controller.cancel(UUID.randomUUID(), body, manager));
    }

    @Test
    void cancel_ok() {
        UUID id = UUID.randomUUID();
        CancelBookingRequest body = new CancelBookingRequest();
        body.setCancelReason("Guest no longer needs the room");
        when(bookingService.cancelBookingForManager(manager, id, body.resolvedReason()))
                .thenReturn(new ManagerBookingDetailResponse());
        assertEquals(HttpStatus.OK, controller.cancel(id, body, manager).getStatusCode());
    }

    @Test
    void modifyCheckInOutUpload() {
        UUID id = UUID.randomUUID();
        ModifyBookingResponse modify = new ModifyBookingResponse();
        modify.setMessage("ok");
        when(bookingService.modifyBookingForManager(eq(manager), eq(id), any())).thenReturn(modify);
        controller.modify(id, new ModifyBookingRequest(), manager);

        when(bookingService.markAsCheckedInForManager(eq(manager), eq(id), any()))
                .thenReturn(new ManagerBookingDetailResponse());
        controller.checkIn(id, new ManagerCheckInRequest(), manager);

        ManagerBookingDetailResponse co = new ManagerBookingDetailResponse();
        co.setStatus("CHECKED_OUT");
        when(bookingService.markAsCheckedOutForManager(eq(manager), eq(id), any())).thenReturn(co);
        controller.checkOut(id, new ManagerCheckOutRequest(), manager);

        when(bookingService.uploadIdDocumentsForManager(eq(manager), eq(id), any())).thenReturn(List.of("/x"));
        controller.uploadIdDocuments(id, List.of(), manager);
        verify(bookingService).uploadIdDocumentsForManager(eq(manager), eq(id), any());
    }
}
