package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO response cho SCR-45 — Admin Dashboard KPI toàn hệ thống.
 * Endpoint: GET /api/reports/global-kpis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalKpisResponse {

    /** Tổng doanh thu toàn hệ thống (payment PAID, mọi thời điểm) */
    private long totalRevenue;

    /** Tổng số booking */
    private long totalBookings;

    /** Tổng số property */
    private long totalProperties;

    /** Tổng số floor */
    private long totalFloors;

    /** Tổng số room */
    private long totalRooms;

    /** Phòng đang available */
    private long availableRooms;

    /** Phòng đang occupied */
    private long occupiedRooms;

    /** Tổng số khách hàng (role CUSTOMER) */
    private long totalCustomers;

    private long upcomingCheckIns;
    private long upcomingCheckOuts;
    private long monthlyRevenue;
}
