package com.homestay.controller;

import com.homestay.dto.response.BookingDetailResponse;
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
class BookingControllerTest {

    @Mock BookingService bookingService;

    private BookingController controller;

    @BeforeEach
    void setUp() {
        controller = new BookingController(bookingService);
    }

    @Test
    void getBookings_manager() {
        User manager = TestFixtures.user(User.Role.MANAGER);
        when(bookingService.getAllBookings(0, 10, null, null, "createdAt,desc"))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0));
        assertEquals(HttpStatus.OK, controller.getBookings(manager, 0, 10, null, null, "createdAt,desc").getStatusCode());
    }

    @Test
    void getBookings_customer() {
        User customer = TestFixtures.user(User.Role.CUSTOMER);
        when(bookingService.getMyBookings(eq(customer), eq(0), eq(10), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0));
        controller.getBookings(customer, 0, 10, null, null, "createdAt,desc");
        verify(bookingService).getMyBookings(customer, 0, 10, null, "createdAt,desc");
    }

    @Test
    void getDetailAndCancel() {
        User customer = TestFixtures.user(User.Role.CUSTOMER);
        UUID id = UUID.randomUUID();
        when(bookingService.getBookingDetail(id, customer)).thenReturn(new BookingDetailResponse());
        controller.getBookingDetail(id, customer);
        controller.cancelBooking(id, customer);
        verify(bookingService).cancelBooking(id, customer);
    }

    @Test
    void legacyCheckInOut_blocked() {
        UUID id = UUID.randomUUID();
        assertThrows(BusinessException.class, () -> controller.checkIn(id));
        assertThrows(BusinessException.class, () -> controller.checkOut(id));
    }
}
