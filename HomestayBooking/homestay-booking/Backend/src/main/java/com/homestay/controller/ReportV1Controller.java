package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.BookingStatusCountResponse;
import com.homestay.dto.response.BookingTrendReportResponse;
import com.homestay.dto.response.OccupancyReportResponse;
import com.homestay.dto.response.PropertyKpisResponse;
import com.homestay.dto.response.RevenueReportResponse;
import com.homestay.entity.User;
import com.homestay.service.PropertyKpisService;
import com.homestay.service.PropertyReportService;
import com.homestay.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** SCR-27 & SCR-44 — Manager reporting v1 endpoints (FR-16). */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportV1Controller {

    private final PropertyKpisService propertyKpisService;
    private final PropertyReportService propertyReportService;
    private final ReportService reportService;

    @GetMapping("/property-kpis")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PropertyKpisResponse>> getPropertyKpis(
            @RequestParam UUID propertyId,
            @AuthenticationPrincipal User currentUser) {
        PropertyKpisResponse data = propertyKpisService.getPropertyKpis(currentUser, propertyId);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /** SCR-44 / SCR-59 — Doanh thu theo property (bắt buộc propertyId, FR-011). */
    @GetMapping("/revenue")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> getRevenue(
            @RequestParam UUID propertyId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "month") String groupBy,
            @AuthenticationPrincipal User currentUser) {
        ResponseEntity<ApiResponse<RevenueReportResponse>> invalid = validateRangeOptional(from, to, groupBy);
        if (invalid != null) return invalid;
        RevenueReportResponse data = reportService.getRevenueReportForManager(
                currentUser, propertyId, from, to, groupBy);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/booking-status-breakdown")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<BookingStatusCountResponse>>> getBookingStatusBreakdown(
            @RequestParam UUID propertyId,
            @AuthenticationPrincipal User currentUser) {
        List<BookingStatusCountResponse> data =
                propertyKpisService.getBookingStatusBreakdown(currentUser, propertyId);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /** SCR-44 — Tỷ lệ lấp đầy theo kỳ (property-scoped, view-only). */
    @GetMapping("/occupancy")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<OccupancyReportResponse>> getOccupancy(
            @RequestParam UUID propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "month") String groupBy,
            @AuthenticationPrincipal User currentUser) {
        ResponseEntity<ApiResponse<OccupancyReportResponse>> invalid = validateRange(from, to, groupBy);
        if (invalid != null) return invalid;
        OccupancyReportResponse data =
                propertyReportService.getOccupancyReport(currentUser, propertyId, from, to, groupBy);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /** SCR-44 — Xu hướng đặt phòng theo kỳ (property-scoped, view-only). */
    @GetMapping("/booking-trends")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<BookingTrendReportResponse>> getBookingTrends(
            @RequestParam UUID propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "month") String groupBy,
            @AuthenticationPrincipal User currentUser) {
        ResponseEntity<ApiResponse<BookingTrendReportResponse>> invalid = validateRange(from, to, groupBy);
        if (invalid != null) return invalid;
        BookingTrendReportResponse data =
                propertyReportService.getBookingTrendReport(currentUser, propertyId, from, to, groupBy);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /** Trả 400 nếu tham số không hợp lệ, null nếu OK. */
    private <T> ResponseEntity<ApiResponse<T>> validateRange(LocalDate from, LocalDate to, String groupBy) {
        if (!"month".equalsIgnoreCase(groupBy) && !"week".equalsIgnoreCase(groupBy)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("groupBy phải là 'month' hoặc 'week'"));
        }
        if (from.isAfter(to)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("from phải trước hoặc bằng to"));
        }
        return null;
    }

    private <T> ResponseEntity<ApiResponse<T>> validateRangeOptional(LocalDate from, LocalDate to, String groupBy) {
        if (!"month".equalsIgnoreCase(groupBy) && !"week".equalsIgnoreCase(groupBy)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("groupBy phải là 'month' hoặc 'week'"));
        }
        if (from != null && to != null && from.isAfter(to)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("from phải trước hoặc bằng to"));
        }
        return null;
    }
}
