package com.homestay.service;

import com.homestay.dto.response.RevenueReportResponse;
import com.homestay.entity.Payment;
import com.homestay.entity.Property;
import com.homestay.entity.User;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.PaymentRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class ReportServiceCoverageTest {

    @Mock PaymentRepository paymentRepository;
    @Mock ReportPropertyScopeValidator scopeValidator;
    @Mock ManagerPropertyAssignmentRepository assignmentRepository;

    private ReportService service;
    private User manager;
    private Property property;

    @BeforeEach
    void setUp() {
        service = new ReportService(paymentRepository, scopeValidator, assignmentRepository);
        manager = TestFixtures.user(User.Role.MANAGER);
        property = TestFixtures.property();
    }

    @Test
    void getRevenueReport_groupsByMonth() {
        UUID bookingId = UUID.randomUUID();
        Object[] row = new Object[]{
                LocalDateTime.of(2026, 7, 10, 12, 0),
                new BigDecimal("1000"),
                Payment.Type.DEPOSIT,
                property.getId(),
                property.getName(),
                bookingId
        };
        when(paymentRepository.sumRevenueByType(any(), any(), eq(property.getId()), isNull()))
                .thenReturn(new BigDecimal("1000"));
        when(paymentRepository.sumRevenueByType(any(), any(), eq(property.getId()), eq(Payment.Type.DEPOSIT)))
                .thenReturn(new BigDecimal("400"));
        when(paymentRepository.sumRevenueByType(any(), any(), eq(property.getId()), eq(Payment.Type.REMAINING_BALANCE)))
                .thenReturn(new BigDecimal("600"));
        when(paymentRepository.countDistinctBookings(any(), any(), eq(property.getId()))).thenReturn(1L);
        when(paymentRepository.findRevenueRawData(any(), any(), eq(property.getId())))
                .thenReturn(List.<Object[]>of(row));

        RevenueReportResponse res = service.getRevenueReport(
                property.getId(), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "month");
        assertEquals(0, res.getTotalRevenue().compareTo(new BigDecimal("1000")));
        assertEquals(1, res.getByPeriod().size());
        assertEquals("2026-07", res.getByPeriod().get(0).getPeriod());
        assertEquals(1, res.getByProperty().size());
    }

    @Test
    void getRevenueReportForManager_scoped() {
        when(paymentRepository.sumRevenueByType(any(), any(), eq(property.getId()), any()))
                .thenReturn(BigDecimal.ZERO);
        when(paymentRepository.countDistinctBookings(any(), any(), eq(property.getId()))).thenReturn(0L);
        when(paymentRepository.findRevenueRawData(any(), any(), eq(property.getId()))).thenReturn(List.of());

        service.getRevenueReportForManager(manager, property.getId(), null, null, "week");
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
    }

    @Test
    void getRevenueReportForManager_allAssignedEmpty() {
        when(assignmentRepository.findAssignedPropertiesByManagerId(manager.getId())).thenReturn(List.of());
        RevenueReportResponse res = service.getRevenueReportForManager(manager, null, null, null, "month");
        assertEquals(0, res.getTotalBookingCount());
    }

    @Test
    void getRevenueReportForManager_multipleProperties() {
        Property other = TestFixtures.property();
        when(assignmentRepository.findAssignedPropertiesByManagerId(manager.getId()))
                .thenReturn(List.of(property, other));
        when(paymentRepository.sumRevenueByTypeForProperties(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(paymentRepository.countDistinctBookingsForProperties(any(), any(), any())).thenReturn(0L);
        when(paymentRepository.findRevenueRawDataForProperties(any(), any(), any())).thenReturn(List.of());

        RevenueReportResponse res = service.getRevenueReportForManager(
                manager, null, LocalDate.now().minusDays(7), LocalDate.now(), "week");
        assertEquals(0, res.getTotalRevenue().compareTo(BigDecimal.ZERO));
    }
}
