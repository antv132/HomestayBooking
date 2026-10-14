package com.homestay.service;

import com.homestay.dto.response.AssignedPropertyResponse;
import com.homestay.dto.response.BookingStatusCountResponse;
import com.homestay.dto.response.PropertyKpisResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.MaintenanceTicketRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.PaymentRepository;
import com.homestay.repository.RoomRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyKpisServiceTest {

    @Mock ReportPropertyScopeValidator scopeValidator;
    @Mock ManagerPropertyAssignmentRepository assignmentRepository;
    @Mock RoomRepository roomRepository;
    @Mock BookingRepository bookingRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock MaintenanceTicketRepository maintenanceTicketRepository;

    @InjectMocks PropertyKpisService service;

    private User manager;
    private Property property;

    @BeforeEach
    void setUp() {
        manager = TestFixtures.user(User.Role.MANAGER);
        property = TestFixtures.property();
    }

    @Test
    void getMyAssignedProperties() {
        when(assignmentRepository.findActivePropertiesByManagerId(manager.getId()))
                .thenReturn(List.of(property));

        List<AssignedPropertyResponse> list = service.getMyAssignedProperties(manager);
        assertEquals(1, list.size());
        assertEquals(property.getId(), list.get(0).getId());
    }

    @Test
    void getPropertyKpis_computesOccupancyAndPendingApprovals() {
        when(roomRepository.countByPropertyId(property.getId())).thenReturn(10L);
        when(roomRepository.countByPropertyIdAndStatus(property.getId(), Room.Status.OCCUPIED)).thenReturn(4L);
        when(paymentRepository.sumRevenueByType(any(), any(), eq(property.getId()), eq(null)))
                .thenReturn(new BigDecimal("500000"));
        when(bookingRepository.countPendingCheckInsByProperty(
                eq(property.getId()), any(), eq(Booking.Status.CONFIRMED))).thenReturn(2L);
        when(paymentRepository.countPendingByPropertyId(property.getId())).thenReturn(1L);
        when(maintenanceTicketRepository.countOpenByPropertyId(property.getId())).thenReturn(3L);

        PropertyKpisResponse kpis = service.getPropertyKpis(manager, property.getId());

        verify(scopeValidator).validateManagerAccess(manager, property.getId());
        assertEquals(10L, kpis.getTotalRooms());
        assertEquals(40.0, kpis.getOccupancyRate());
        assertEquals(500_000L, kpis.getRevenue());
        assertEquals(2, kpis.getPendingCheckIns());
        assertEquals(4, kpis.getPendingApprovals());
    }

    @Test
    void getPropertyKpis_zeroRooms_occupancyZero() {
        when(roomRepository.countByPropertyId(property.getId())).thenReturn(0L);
        when(roomRepository.countByPropertyIdAndStatus(property.getId(), Room.Status.OCCUPIED)).thenReturn(0L);
        when(paymentRepository.sumRevenueByType(any(), any(), eq(property.getId()), eq(null))).thenReturn(null);
        when(bookingRepository.countPendingCheckInsByProperty(any(), any(), any())).thenReturn(0L);
        when(paymentRepository.countPendingByPropertyId(property.getId())).thenReturn(0L);
        when(maintenanceTicketRepository.countOpenByPropertyId(property.getId())).thenReturn(0L);

        PropertyKpisResponse kpis = service.getPropertyKpis(manager, property.getId());
        assertEquals(0.0, kpis.getOccupancyRate());
        assertEquals(0L, kpis.getRevenue());
    }

    @Test
    void getBookingStatusBreakdown() {
        Object[] row = new Object[]{Booking.Status.CONFIRMED, 5L};
        when(bookingRepository.countByStatusForPropertyInRange(eq(property.getId()), any(), any()))
                .thenReturn(List.<Object[]>of(row));

        List<BookingStatusCountResponse> breakdown =
                service.getBookingStatusBreakdown(manager, property.getId());

        verify(scopeValidator).validateManagerAccess(manager, property.getId());
        assertEquals(1, breakdown.size());
        assertEquals(Booking.Status.CONFIRMED.name(), breakdown.get(0).getStatus());
        assertEquals(5L, breakdown.get(0).getCount());
    }
}
