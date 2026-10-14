package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

// Dữ liệu lịch trống phòng cho SCR-10
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomCalendarResponse {

    private String roomStatus;
    private List<BookedRange> bookedRanges;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookedRange {
        private LocalDate checkIn;
        private LocalDate checkOut;
        private String bookingStatus; // PENDING_DEPOSIT | CONFIRMED | CHECKED_IN
    }
}
