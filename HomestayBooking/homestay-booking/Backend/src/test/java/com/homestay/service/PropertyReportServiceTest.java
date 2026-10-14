package com.homestay.service;

import com.homestay.dto.response.BookingTrendReportResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.User;
import com.homestay.exception.ForbiddenException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.RoomRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyReportServiceTest {

    @Mock ReportPropertyScopeValidator scopeValidator;
    @Mock RoomRepository roomRepository;
    @Mock BookingRepository bookingRepository;

    private PropertyReportService service;
    private User manager;
    private UUID propertyId;

    @BeforeEach
    void setUp() {
        service = new PropertyReportService(scopeValidator, roomRepository, bookingRepository);
        manager = TestFixtures.user(User.Role.MANAGER);
        propertyId = UUID.randomUUID();
    }

    @Test
    void getBookingTrendReport_countsOnlyConfirmedCheckedInCheckedOut() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        when(bookingRepository.findBookingCreationTimes(
                eq(propertyId),
                eq(List.of(Booking.Status.CONFIRMED, Booking.Status.CHECKED_IN, Booking.Status.CHECKED_OUT)),
                any(LocalDateTime.class),
                any(LocalDateTime.class)))
                .thenReturn(List.of(
                        LocalDateTime.of(2026, 1, 5, 10, 0),
                        LocalDateTime.of(2026, 1, 20, 12, 0)));
        when(bookingRepository.countByStatusForPropertyInRange(eq(propertyId), any(), any()))
                .thenReturn(List.of(
                        new Object[]{Booking.Status.CONFIRMED, 2L},
                        new Object[]{Booking.Status.CANCELLED, 5L}));

        BookingTrendReportResponse res = service.getBookingTrendReport(manager, propertyId, from, to, "month");

        assertEquals(2, res.getTotalBookings());
        assertEquals(1, res.getByPeriod().size());
        assertEquals(2, res.getByPeriod().get(0).getBookingCount());
        verify(scopeValidator).validateManagerAccess(manager, propertyId);
    }

    @Test
    void getOccupancyReport_deniesUnassignedProperty() {
        doThrow(new ForbiddenException("denied"))
                .when(scopeValidator).validateManagerAccess(manager, propertyId);

        assertThrows(ForbiddenException.class, () ->
                service.getOccupancyReport(manager, propertyId,
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "month"));
    }

    @Test
    void getOccupancyReportAdmin_skipsManagerScope() {
        when(roomRepository.countByPropertyId(propertyId)).thenReturn(2L);
        when(bookingRepository.findOccupancyRawData(eq(propertyId), any(), any(), any()))
                .thenReturn(List.of());

        var res = service.getOccupancyReportAdmin(
                propertyId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), "month");

        assertEquals(2L, res.getTotalRooms());
        assertEquals(0.0, res.getAvgOccupancyRate());
    }
}
