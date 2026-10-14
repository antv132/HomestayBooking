package com.homestay.controller;

import com.homestay.dto.response.ApiResponse;
import com.homestay.dto.response.RevenueReportResponse;
import com.homestay.entity.User;
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
import java.util.UUID;

/**
 * Legacy Manager revenue report (SCR-59). Prefer {@code /api/v1/reports/revenue}.
 * Property-scoped via assignment (FR-011) — never returns unassigned properties.
 */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * GET /api/reports/revenue
     *
     * @param propertyId optional — null = all assigned properties for the manager
     */
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> getRevenueReport(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "month") String groupBy,
            @AuthenticationPrincipal User currentUser
    ) {
        if (!"month".equalsIgnoreCase(groupBy) && !"week".equalsIgnoreCase(groupBy)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("groupBy phải là 'month' hoặc 'week'"));
        }
        if (from != null && to != null && from.isAfter(to)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("from phải trước hoặc bằng to"));
        }

        RevenueReportResponse data = reportService.getRevenueReportForManager(
                currentUser, propertyId, from, to, groupBy);
        return ResponseEntity.ok(ApiResponse.ok("Success", data));
    }
}
