package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO response cho SCR-45 — Doanh thu theo tháng toàn hệ thống (bar chart).
 * Endpoint: GET /api/admin/reports/revenue?year=
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalRevenueReportResponse {

    /** Doanh thu 12 tháng (month 1..12), tháng thiếu dữ liệu = 0 */
    private List<MonthlyRevenue> monthlyData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenue {
        /** Tháng 1..12 */
        private int month;
        /** Tổng doanh thu tháng đó */
        private long revenue;
    }
}
