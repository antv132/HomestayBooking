package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

// Trả về cho calendar trên trang chi tiết phòng (SCR-10)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {

    private boolean available;  // true nếu không có booking trùng
    private List<DateRange> bookedRanges; // các khoảng ngày đã bị đặt

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRange {
        private LocalDate checkIn;
        private LocalDate checkOut;
    }
}
