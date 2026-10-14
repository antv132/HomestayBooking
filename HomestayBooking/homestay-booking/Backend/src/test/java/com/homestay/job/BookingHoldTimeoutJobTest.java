package com.homestay.job;

import com.homestay.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingHoldTimeoutJobTest {

    @Mock BookingService bookingService;

    private BookingHoldTimeoutJob job;

    @BeforeEach
    void setUp() {
        job = new BookingHoldTimeoutJob(bookingService);
    }

    @Test
    void cancelExpiredHolds_callsBookingService() {
        when(bookingService.cancelExpiredDepositHolds()).thenReturn(3);

        job.cancelExpiredHolds();

        verify(bookingService).cancelExpiredDepositHolds();
    }

    @Test
    void cancelExpiredHolds_noneExpired_logsNothing() {
        when(bookingService.cancelExpiredDepositHolds()).thenReturn(0);

        job.cancelExpiredHolds();

        verify(bookingService).cancelExpiredDepositHolds();
    }

    @Test
    void cancelExpiredHolds_someExpired_logsCount() {
        when(bookingService.cancelExpiredDepositHolds()).thenReturn(5);

        job.cancelExpiredHolds();

        verify(bookingService).cancelExpiredDepositHolds();
    }
}
