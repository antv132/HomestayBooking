package com.homestay.service;

import com.homestay.dto.response.GlobalKpisResponse;
import com.homestay.dto.response.GlobalRevenueReportResponse;
import com.homestay.dto.response.RevenueReportResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.FloorRepository;
import com.homestay.repository.PaymentRepository;
import com.homestay.repository.PropertyRepository;
import com.homestay.repository.RoomRepository;
import com.homestay.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock BookingRepository bookingRepository;
    @Mock PropertyRepository propertyRepository;
    @Mock FloorRepository floorRepository;
    @Mock RoomRepository roomRepository;
    @Mock UserRepository userRepository;
    @Mock ReportService reportService;

    @InjectMocks AdminDashboardService service;

    @Test
    void getGlobalKpis_mapsAggregates() {
        when(paymentRepository.sumRevenueByType(any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    LocalDateTime from = inv.getArgument(0);
                    return from == null
                            ? new BigDecimal("1000000")
                            : new BigDecimal("250000");
                });
        when(bookingRepository.count()).thenReturn(12L);
        when(propertyRepository.count()).thenReturn(3L);
        when(floorRepository.count()).thenReturn(6L);
        when(roomRepository.count()).thenReturn(20L);
        when(roomRepository.countByStatus(Room.Status.AVAILABLE)).thenReturn(8L);
        when(roomRepository.countByStatus(Room.Status.OCCUPIED)).thenReturn(5L);
        when(userRepository.countByRole(User.Role.CUSTOMER)).thenReturn(40L);
        when(bookingRepository.countByStatusAndCheckInDateGreaterThanEqual(
                eq(Booking.Status.CONFIRMED), any(LocalDate.class))).thenReturn(2L);
        when(bookingRepository.countByStatusAndCheckOutDateGreaterThanEqual(
                eq(Booking.Status.CHECKED_IN), any(LocalDate.class))).thenReturn(1L);

        GlobalKpisResponse kpis = service.getGlobalKpis();

        assertEquals(1_000_000L, kpis.getTotalRevenue());
        assertEquals(250_000L, kpis.getMonthlyRevenue());
        assertEquals(12L, kpis.getTotalBookings());
        assertEquals(3L, kpis.getTotalProperties());
        assertEquals(8L, kpis.getAvailableRooms());
        assertEquals(5L, kpis.getOccupiedRooms());
        assertEquals(40L, kpis.getTotalCustomers());
        assertEquals(2L, kpis.getUpcomingCheckIns());
        assertEquals(1L, kpis.getUpcomingCheckOuts());
    }

    @Test
    void getGlobalRevenueReport_fillsTwelveMonths() {
        RevenueReportResponse.PeriodRevenue jan = new RevenueReportResponse.PeriodRevenue();
        jan.setPeriod("2026-01");
        jan.setRevenue(new BigDecimal("100"));
        RevenueReportResponse.PeriodRevenue mar = new RevenueReportResponse.PeriodRevenue();
        mar.setPeriod("2026-03");
        mar.setRevenue(new BigDecimal("300"));

        RevenueReportResponse report = new RevenueReportResponse();
        report.setByPeriod(List.of(jan, mar));
        when(reportService.getRevenueReport(isNull(), any(), any(), eq("month"))).thenReturn(report);

        GlobalRevenueReportResponse global = service.getGlobalRevenueReport(2026, null);

        assertEquals(12, global.getMonthlyData().size());
        assertEquals(100L, global.getMonthlyData().get(0).getRevenue());
        assertEquals(0L, global.getMonthlyData().get(1).getRevenue());
        assertEquals(300L, global.getMonthlyData().get(2).getRevenue());
    }

    @Test
    void getGlobalRevenueReport_withPropertyFilter() {
        UUID propertyId = UUID.randomUUID();
        RevenueReportResponse report = new RevenueReportResponse();
        report.setByPeriod(List.of());
        when(reportService.getRevenueReport(eq(propertyId), any(), any(), eq("month"))).thenReturn(report);

        GlobalRevenueReportResponse global = service.getGlobalRevenueReport(2026, propertyId);
        assertEquals(12, global.getMonthlyData().size());
        verify(reportService).getRevenueReport(eq(propertyId), any(), any(), eq("month"));
    }
}
