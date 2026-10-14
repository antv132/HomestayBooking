package com.homestay.job;

import com.homestay.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Marks CONFIRMED bookings as NO_SHOW when more than 24h past expected check-in
 * (FR-008). Deposit is kept; inventory released via status (overlap excludes NO_SHOW).
 */
@Component
public class BookingNoShowJob {

    private static final Logger log = LoggerFactory.getLogger(BookingNoShowJob.class);

    private final BookingService bookingService;

    public BookingNoShowJob(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelayString = "${app.booking.no-show-job-ms:60000}")
    public void markNoShows() {
        int marked = bookingService.markNoShowExpiredConfirmed();
        if (marked > 0) {
            log.info("No-show: marked {} CONFIRMED booking(s) as NO_SHOW", marked);
        }
    }
}
