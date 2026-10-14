package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO response cho SCR-44 — Booking Trend Report (Manager, view-only).
 * Endpoint: GET /api/v1/reports/booking-trends
 *
 * Đếm số booking theo kỳ (theo createdAt) cho 1 property + breakdown theo status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingTrendReportResponse {

    /** Tổng số booking được tạo trong khoảng */
    private long totalBookings;

    /** Số booking theo kỳ (tháng/tuần) — dùng cho Line Chart */
    private List<PeriodBookingCount> byPeriod;

    /** Phân bổ booking theo trạng thái trong khoảng */
    private List<StatusCount> byStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodBookingCount {
        /** Kỳ: "2026-01" (month) hoặc "2026-W22" (week) */
        private String period;
        /** Số booking tạo trong kỳ */
        private long bookingCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusCount {
        private String status;
        private long count;
    }
}
