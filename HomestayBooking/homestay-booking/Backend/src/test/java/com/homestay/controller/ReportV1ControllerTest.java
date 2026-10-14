package com.homestay.controller;

import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.PropertyKpisResponse;
import com.homestay.dto.response.RevenueReportResponse;
import com.homestay.entity.User;
import com.homestay.service.PropertyKpisService;
import com.homestay.service.PropertyReportService;
import com.homestay.service.ReportService;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportV1ControllerTest {

    @Mock PropertyKpisService propertyKpisService;
    @Mock PropertyReportService propertyReportService;
    @Mock ReportService reportService;

    private ReportV1Controller controller;
    private User manager;
    private UUID propertyId;

    @BeforeEach
    void setUp() {
        controller = new ReportV1Controller(propertyKpisService, propertyReportService, reportService);
        manager = TestFixtures.user(User.Role.MANAGER);
        propertyId = UUID.randomUUID();
    }

    @Test
    void getPropertyKpis() {
        when(propertyKpisService.getPropertyKpis(manager, propertyId)).thenReturn(new PropertyKpisResponse());
        assertEquals(HttpStatus.OK, controller.getPropertyKpis(propertyId, manager).getStatusCode());
    }

    @Test
    void getRevenue_ok() {
        when(reportService.getRevenueReportForManager(eq(manager), eq(propertyId), any(), any(), eq("month")))
                .thenReturn(new RevenueReportResponse());
        assertEquals(HttpStatus.OK,
                controller.getRevenue(propertyId, LocalDate.now().minusDays(30), LocalDate.now(), "month", manager)
                        .getStatusCode());
    }

    @Test
    void getBookingStatusBreakdown() {
        when(propertyKpisService.getBookingStatusBreakdown(manager, propertyId)).thenReturn(List.of());
        assertEquals(HttpStatus.OK,
                controller.getBookingStatusBreakdown(propertyId, manager).getStatusCode());
    }

    @Test
    void getOccupancy() {
        LocalDate from = LocalDate.now().minusDays(7);
        LocalDate to = LocalDate.now();
        controller.getOccupancy(propertyId, from, to, "week", manager);
        verify(propertyReportService).getOccupancyReport(manager, propertyId, from, to, "week");
    }

    @Test
    void getBookingTrends() {
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to = LocalDate.now();
        controller.getBookingTrends(propertyId, from, to, "month", manager);
        verify(propertyReportService).getBookingTrendReport(manager, propertyId, from, to, "month");
    }

    @Test
    void getOccupancy_invalidGroupBy() {
        assertEquals(HttpStatus.BAD_REQUEST,
                controller.getOccupancy(propertyId, LocalDate.now(), LocalDate.now(), "day", manager)
                        .getStatusCode());
    }
}
