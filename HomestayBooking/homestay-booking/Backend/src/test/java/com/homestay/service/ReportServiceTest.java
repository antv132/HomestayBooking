package com.homestay.service;

import com.homestay.dto.response.RevenueReportResponse;
import com.homestay.entity.Payment;
import com.homestay.entity.Property;
import com.homestay.entity.User;
import com.homestay.exception.ForbiddenException;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock ReportPropertyScopeValidator scopeValidator;
    @Mock ManagerPropertyAssignmentRepository assignmentRepository;

    private ReportService service;
    private User manager;
    private UUID propertyId;

    @BeforeEach
    void setUp() {
        service = new ReportService(paymentRepository, scopeValidator, assignmentRepository);
        manager = TestFixtures.user(User.Role.MANAGER);
        propertyId = UUID.randomUUID();
    }

    private void stubSinglePropertySums(UUID pid, BigDecimal total) {
        when(paymentRepository.sumRevenueByType(any(), any(), eq(pid), isNull())).thenReturn(total);
        when(paymentRepository.sumRevenueByType(any(), any(), eq(pid), eq(Payment.Type.DEPOSIT)))
                .thenReturn(BigDecimal.ZERO);
        when(paymentRepository.sumRevenueByType(any(), any(), eq(pid), eq(Payment.Type.REMAINING_BALANCE)))
                .thenReturn(BigDecimal.ZERO);
        when(paymentRepository.countDistinctBookings(any(), any(), eq(pid))).thenReturn(1L);
        when(paymentRepository.findRevenueRawData(any(), any(), eq(pid))).thenReturn(List.of());
    }

    @Test
    void getRevenueReportForManager_withProperty_validatesScope() {
        stubSinglePropertySums(propertyId, BigDecimal.valueOf(1000));

        RevenueReportResponse res = service.getRevenueReportForManager(
                manager, propertyId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "month");

        assertEquals(0, BigDecimal.valueOf(1000).compareTo(res.getTotalRevenue()));
        verify(scopeValidator).validateManagerAccess(manager, propertyId);
    }

    @Test
    void getRevenueReportForManager_unassigned_throwsForbidden() {
        doThrow(new ForbiddenException("denied"))
                .when(scopeValidator).validateManagerAccess(manager, propertyId);

        assertThrows(ForbiddenException.class, () ->
                service.getRevenueReportForManager(
                        manager, propertyId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "month"));
        verify(paymentRepository, never()).sumRevenueByType(any(), any(), any(), any());
    }

    @Test
    void getRevenueReportForManager_nullProperty_usesAssignedOnly() {
        Property p = TestFixtures.property();
        when(assignmentRepository.findAssignedPropertiesByManagerId(manager.getId()))
                .thenReturn(List.of(p));
        stubSinglePropertySums(p.getId(), BigDecimal.TEN);

        RevenueReportResponse res = service.getRevenueReportForManager(
                manager, null, null, null, "month");

        assertEquals(0, BigDecimal.TEN.compareTo(res.getTotalRevenue()));
        verify(scopeValidator, never()).validateManagerAccess(any(), any());
    }
}
