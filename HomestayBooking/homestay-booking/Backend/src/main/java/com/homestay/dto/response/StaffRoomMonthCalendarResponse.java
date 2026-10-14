package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FR-05 US2/US4 — Manager/Employee month calendar.
 * Reuses the same {@code days[]} model as public SCR-09 ({@link MonthAvailabilityResponse}).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffRoomMonthCalendarResponse {

    private UUID roomId;
    private String roomNumber;
    private String roomStatus;
    private List<CalendarDayStatusResponse> days = new ArrayList<>();
    /** FR-03 compat flat lists */
    private List<String> bookedDates = new ArrayList<>();
    private List<String> maintenanceDates = new ArrayList<>();
    /** Booking context for tooltips (SCR-33); guest calendar does not expose this. */
    private List<BookingRangeRef> bookedRanges = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingRangeRef {
        private UUID bookingId;
        private String checkIn;
        private String checkOut;
        private String bookingStatus;
    }
}
