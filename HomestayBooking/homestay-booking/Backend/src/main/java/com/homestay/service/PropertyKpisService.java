package com.homestay.service;

import com.homestay.dto.response.AssignedPropertyResponse;
import com.homestay.dto.response.BookingStatusCountResponse;
import com.homestay.dto.response.PropertyKpisResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.MaintenanceTicketRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.PaymentRepository;
import com.homestay.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** SCR-27 — Property-scoped KPI aggregation for Manager Dashboard. */
@Service
@RequiredArgsConstructor
public class PropertyKpisService {

    private final ReportPropertyScopeValidator scopeValidator;
    private final ManagerPropertyAssignmentRepository assignmentRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final MaintenanceTicketRepository maintenanceTicketRepository;

    @Transactional(readOnly = true)
    public List<AssignedPropertyResponse> getMyAssignedProperties(User manager) {
        return assignmentRepository.findActivePropertiesByManagerId(manager.getId()).stream()
                .map(AssignedPropertyResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PropertyKpisResponse getPropertyKpis(User manager, UUID propertyId) {
        scopeValidator.validateManagerAccess(manager, propertyId);

        long totalRooms = roomRepository.countByPropertyId(propertyId);
        long occupiedRooms = roomRepository.countByPropertyIdAndStatus(propertyId, Room.Status.OCCUPIED);
        double occupancyRate = totalRooms == 0
                ? 0.0
                : BigDecimal.valueOf(occupiedRooms * 100.0 / totalRooms)
                        .setScale(1, RoundingMode.HALF_UP)
                        .doubleValue();

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime from = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime to = currentMonth.atEndOfMonth().atTime(23, 59, 59, 999_999_999);

        BigDecimal revenueSum = paymentRepository.sumRevenueByType(from, to, propertyId, null);
        long revenue = revenueSum != null ? revenueSum.longValue() : 0L;

        int pendingCheckIns = (int) bookingRepository.countPendingCheckInsByProperty(
                propertyId, LocalDate.now(), Booking.Status.CONFIRMED);

        int pendingPayments = (int) paymentRepository.countPendingByPropertyId(propertyId);
        int openMaintenance = (int) maintenanceTicketRepository.countOpenByPropertyId(propertyId);

        return PropertyKpisResponse.builder()
                .propertyId(propertyId)
                .totalRooms(totalRooms)
                .occupancyRate(occupancyRate)
                .revenue(revenue)
                .pendingCheckIns(pendingCheckIns)
                .pendingApprovals(pendingPayments + openMaintenance)
                .build();
    }

    @Transactional(readOnly = true)
    public List<BookingStatusCountResponse> getBookingStatusBreakdown(User manager, UUID propertyId) {
        scopeValidator.validateManagerAccess(manager, propertyId);

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime from = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime to = currentMonth.atEndOfMonth().atTime(23, 59, 59, 999_999_999);

        return bookingRepository.countByStatusForPropertyInRange(propertyId, from, to).stream()
                .map(row -> new BookingStatusCountResponse(
                        ((Booking.Status) row[0]).name(),
                        ((Number) row[1]).longValue()))
                .toList();
    }
}
