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
class BookingNoShowJobTest {

    @Mock BookingService bookingService;

    private BookingNoShowJob job;

    @BeforeEach
    void setUp() {
        job = new BookingNoShowJob(bookingService);
    }

    @Test
    void markNoShows_callsBookingService() {
        when(bookingService.markNoShowExpiredConfirmed()).thenReturn(2);

        job.markNoShows();

        verify(bookingService).markNoShowExpiredConfirmed();
    }

    @Test
    void markNoShows_noneExpired_logsNothing() {
        when(bookingService.markNoShowExpiredConfirmed()).thenReturn(0);

        job.markNoShows();

        verify(bookingService).markNoShowExpiredConfirmed();
    }

    @Test
    void markNoShows_someExpired_logsCount() {
        when(bookingService.markNoShowExpiredConfirmed()).thenReturn(10);

        job.markNoShows();

        verify(bookingService).markNoShowExpiredConfirmed();
    }
}
