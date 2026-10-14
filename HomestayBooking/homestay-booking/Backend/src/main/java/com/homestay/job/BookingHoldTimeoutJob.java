package com.homestay.job;

import com.homestay.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cancels unpaid PENDING_DEPOSIT bookings past holdExpiresAt (default 10 min).
 * Releases date-range inventory so another customer can book the same nights.
 */
@Component
public class BookingHoldTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(BookingHoldTimeoutJob.class);

    private final BookingService bookingService;

    public BookingHoldTimeoutJob(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelayString = "${app.booking.hold-timeout-job-ms:60000}")
    public void cancelExpiredHolds() {
        int cancelled = bookingService.cancelExpiredDepositHolds();
        if (cancelled > 0) {
            log.info("Hold timeout: cancelled {} unpaid PENDING_DEPOSIT booking(s)", cancelled);
        }
    }
}
