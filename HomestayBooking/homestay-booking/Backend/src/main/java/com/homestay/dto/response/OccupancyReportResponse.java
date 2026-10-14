package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO response cho SCR-44 — Occupancy Report (Manager, view-only).
 * Endpoint: GET /api/v1/reports/occupancy
 *
 * Tỷ lệ lấp đầy tính theo đêm-phòng:
 *   occupancyRate = occupiedRoomNights / availableRoomNights * 100
 *   availableRoomNights = totalRooms * số ngày trong kỳ (thuộc khoảng [from, to])
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OccupancyReportResponse {

    /** Tổng số phòng của property */
    private long totalRooms;

    /** Tỷ lệ lấp đầy trung bình toàn kỳ (%) */
    private double avgOccupancyRate;

    /** Tổng số đêm-phòng bị chiếm trong toàn khoảng */
    private long totalOccupiedRoomNights;

    /** Tổng số đêm-phòng khả dụng trong toàn khoảng */
    private long totalAvailableRoomNights;

    /** Tỷ lệ lấp đầy theo kỳ (tháng/tuần) — dùng cho Line Chart */
    private List<PeriodOccupancy> byPeriod;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodOccupancy {
        /** Kỳ: "2026-01" (month) hoặc "2026-W22" (week) */
        private String period;
        /** Tỷ lệ lấp đầy kỳ đó (%) */
        private double occupancyRate;
        /** Số đêm-phòng bị chiếm trong kỳ */
        private long occupiedRoomNights;
        /** Số đêm-phòng khả dụng trong kỳ */
        private long availableRoomNights;
    }
}
