package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO response cho SCR-59 — Revenue Report.
 * Endpoint: GET /api/reports/revenue
 *
 * Trả về:
 *   - KPI tổng doanh thu, đặt cọc, phần còn lại, số booking
 *   - Doanh thu theo kỳ (month/week) — dùng cho Bar Chart
 *   - Doanh thu theo property — dùng cho Property Breakdown Chart
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportResponse {

    // ── KPI ─────────────────────────────────────────────────────────────────────

    /** Tổng doanh thu (DEPOSIT + REMAINING_BALANCE, status=PAID) */
    private BigDecimal totalRevenue;

    /** Doanh thu từ đặt cọc 40% */
    private BigDecimal depositRevenue;

    /** Doanh thu từ thanh toán phần còn lại 60% */
    private BigDecimal balanceRevenue;

    /** Số booking đã có ít nhất 1 payment PAID trong kỳ */
    private long totalBookingCount;

    // ── Chart data ──────────────────────────────────────────────────────────────

    /** Doanh thu theo kỳ (tháng hoặc tuần) — dùng cho Bar Chart */
    private List<PeriodRevenue> byPeriod;

    /** Doanh thu theo property — dùng cho Horizontal Bar */
    private List<PropertyRevenue> byProperty;

    // ── Nested DTOs ──────────────────────────────────────────────────────────────

    /**
     * Doanh thu theo 1 kỳ (tháng: "2026-01" | tuần: "2026-W22")
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodRevenue {
        /** Kỳ: "2026-01" (month) hoặc "2026-W22" (week) */
        private String period;
        /** Tổng doanh thu kỳ đó */
        private BigDecimal revenue;
        /** Số booking có payment PAID trong kỳ */
        private long bookingCount;
    }

    /**
     * Doanh thu tổng hợp theo từng property
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyRevenue {
        private String propertyId;
        private String propertyName;
        /** Tổng doanh thu của property */
        private BigDecimal revenue;
        /** Số booking */
        private long bookingCount;
    }
}
