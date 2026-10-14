package com.homestay.controller;

import com.homestay.dto.request.CancelBookingRequest;
import com.homestay.dto.request.CreateBookingRequest;
import com.homestay.dto.response.BookingDetailResponse;
import com.homestay.dto.response.CancellationPreviewResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.User;
import com.homestay.exception.ForbiddenException;
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
class BookingV1ControllerTest {

    @Mock BookingService bookingService;

    private BookingV1Controller controller;
    private User customer;

    @BeforeEach
    void setUp() {
        controller = new BookingV1Controller(bookingService);
        customer = TestFixtures.user(User.Role.CUSTOMER);
    }

    @Test
    void createBooking_requiresAuth() {
        assertThrows(ForbiddenException.class, () ->
                controller.createBooking(new CreateBookingRequest(), null));
    }

    @Test
    void createBooking_ok() {
        when(bookingService.createBooking(any(), eq(customer))).thenReturn(new BookingDetailResponse());
        assertEquals(HttpStatus.CREATED,
                controller.createBooking(new CreateBookingRequest(), customer).getStatusCode());
    }

    @Test
    void myBookingsAndDetail() {
        when(bookingService.getMyBookings(eq(customer), eq(0), eq(10), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0));
        controller.getMyBookings(customer, 0, 10, null, "createdAt,desc");

        UUID id = UUID.randomUUID();
        when(bookingService.getBookingDetail(id, customer)).thenReturn(new BookingDetailResponse());
        controller.getMyBookingDetail(id, customer);

        when(bookingService.getCancellationPreview(id, customer)).thenReturn(new CancellationPreviewResponse());
        controller.getCancellationPreview(id, customer);

        controller.cancelMyBooking(id, customer, new CancelBookingRequest());
        verify(bookingService).cancelBooking(eq(id), eq(customer), any());
    }

    @Test
    void endpoints_rejectNullUser() {
        assertThrows(ForbiddenException.class, () -> controller.getMyBookings(null, 0, 10, null, "createdAt,desc"));
        assertThrows(ForbiddenException.class, () -> controller.getMyBookingDetail(UUID.randomUUID(), null));
        assertThrows(ForbiddenException.class, () -> controller.getCancellationPreview(UUID.randomUUID(), null));
        assertThrows(ForbiddenException.class, () -> controller.cancelMyBooking(UUID.randomUUID(), null, null));
    }
}
