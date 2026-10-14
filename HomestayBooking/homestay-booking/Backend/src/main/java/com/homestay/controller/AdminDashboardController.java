package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.BookingTrendReportResponse;
import com.homestay.dto.response.GlobalKpisResponse;
import com.homestay.dto.response.GlobalRevenueReportResponse;
import com.homestay.dto.response.OccupancyReportResponse;
import com.homestay.service.AdminDashboardService;
import com.homestay.service.PropertyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * SCR-45 / SCR-55 — Admin Dashboard + Global Reports (ADMIN only, view-only).
 */
@RestController
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final PropertyReportService propertyReportService;

    /** SCR-45 — KPI toàn hệ thống. */
    @GetMapping("/api/reports/global-kpis")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GlobalKpisResponse>> getGlobalKpis() {
        return ResponseEntity.ok(ApiResponse.ok(adminDashboardService.getGlobalKpis()));
    }

    /** SCR-55 — Doanh thu theo tháng; optional propertyId filter (US4 / FR-007). */
    @GetMapping("/api/admin/reports/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<GlobalRevenueReportResponse>> getGlobalRevenueReport(
            @RequestParam int year,
            @RequestParam(required = false) UUID propertyId) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminDashboardService.getGlobalRevenueReport(year, propertyId)));
    }

    /**
     * SCR-55 — Occupancy theo năm; propertyId bắt buộc (aggregate per property).
     */
    @GetMapping("/api/admin/reports/occupancy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OccupancyReportResponse>> getAdminOccupancy(
            @RequestParam int year,
            @RequestParam UUID propertyId) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        OccupancyReportResponse data =
                propertyReportService.getOccupancyReportAdmin(propertyId, from, to, "month");
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * SCR-55 — Booking trends theo năm; propertyId bắt buộc; counted statuses only.
     */
    @GetMapping("/api/admin/reports/booking-trends")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BookingTrendReportResponse>> getAdminBookingTrends(
            @RequestParam int year,
            @RequestParam UUID propertyId) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        BookingTrendReportResponse data =
                propertyReportService.getBookingTrendReportAdmin(propertyId, from, to, "month");
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
