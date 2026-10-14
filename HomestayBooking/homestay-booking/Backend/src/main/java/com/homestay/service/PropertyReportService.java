package com.homestay.service;

import com.homestay.dto.response.BookingTrendReportResponse;
import com.homestay.dto.response.BookingTrendReportResponse.PeriodBookingCount;
import com.homestay.dto.response.BookingTrendReportResponse.StatusCount;
import com.homestay.dto.response.OccupancyReportResponse;
import com.homestay.dto.response.OccupancyReportResponse.PeriodOccupancy;
import com.homestay.entity.Booking;
import com.homestay.entity.User;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SCR-44 — Property Reports (Occupancy + Booking Trend) cho Manager, view-only.
 * Property-scoped qua {@link ReportPropertyScopeValidator}.
 * Không đụng ReportService / PropertyKpisService.
 */
@Service
@RequiredArgsConstructor
public class PropertyReportService {

    private final ReportPropertyScopeValidator scopeValidator;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    /** Occupancy + Booking Trend counted statuses (FR-010 / research). */
    private static final List<Booking.Status> COUNTED_STATUSES = List.of(
            Booking.Status.CONFIRMED,
            Booking.Status.CHECKED_IN,
            Booking.Status.CHECKED_OUT);

    // ── Occupancy Report ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public OccupancyReportResponse getOccupancyReport(User manager, UUID propertyId,
                                                      LocalDate from, LocalDate to, String groupBy) {
        scopeValidator.validateManagerAccess(manager, propertyId);
        return computeOccupancy(propertyId, from, to, groupBy);
    }

    /** Admin / SCR-55 — không check Manager assignment. {@code propertyId} bắt buộc. */
    @Transactional(readOnly = true)
    public OccupancyReportResponse getOccupancyReportAdmin(UUID propertyId,
                                                           LocalDate from, LocalDate to, String groupBy) {
        return computeOccupancy(propertyId, from, to, groupBy);
    }

    private OccupancyReportResponse computeOccupancy(UUID propertyId,
                                                     LocalDate from, LocalDate to, String groupBy) {
        boolean isWeek = "week".equalsIgnoreCase(groupBy);
        long totalRooms = roomRepository.countByPropertyId(propertyId);

        // Seed buckets theo thứ tự thời gian: [occupiedNights, availableDays]
        LinkedHashMap<String, long[]> buckets = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            buckets.computeIfAbsent(periodKey(d, isWeek), k -> new long[2])[1] += 1;
        }

        // Đêm-phòng bị chiếm: phân bổ từng đêm của booking vào kỳ tương ứng
        LocalDate rangeEndExclusive = to.plusDays(1);
        List<Object[]> raw = bookingRepository.findOccupancyRawData(
                propertyId, COUNTED_STATUSES, from, rangeEndExclusive);

        for (Object[] row : raw) {
            LocalDate checkIn = (LocalDate) row[0];
            LocalDate checkOut = (LocalDate) row[1];
            LocalDate nightStart = checkIn.isBefore(from) ? from : checkIn;
            LocalDate nightEndExclusive = checkOut.isAfter(rangeEndExclusive) ? rangeEndExclusive : checkOut;
            for (LocalDate d = nightStart; d.isBefore(nightEndExclusive); d = d.plusDays(1)) {
                long[] bucket = buckets.get(periodKey(d, isWeek));
                if (bucket != null) bucket[0] += 1;
            }
        }

        List<PeriodOccupancy> byPeriod = new ArrayList<>();
        long totalOccupied = 0;
        long totalAvailable = 0;
        for (Map.Entry<String, long[]> e : buckets.entrySet()) {
            long occupied = e.getValue()[0];
            long available = e.getValue()[1] * totalRooms;
            totalOccupied += occupied;
            totalAvailable += available;
            byPeriod.add(PeriodOccupancy.builder()
                    .period(e.getKey())
                    .occupancyRate(rate(occupied, available))
                    .occupiedRoomNights(occupied)
                    .availableRoomNights(available)
                    .build());
        }

        return OccupancyReportResponse.builder()
                .totalRooms(totalRooms)
                .avgOccupancyRate(rate(totalOccupied, totalAvailable))
                .totalOccupiedRoomNights(totalOccupied)
                .totalAvailableRoomNights(totalAvailable)
                .byPeriod(byPeriod)
                .build();
    }

    // ── Booking Trend Report ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BookingTrendReportResponse getBookingTrendReport(User manager, UUID propertyId,
                                                            LocalDate from, LocalDate to, String groupBy) {
        scopeValidator.validateManagerAccess(manager, propertyId);
        return computeBookingTrend(propertyId, from, to, groupBy);
    }

    /** Admin / SCR-55 — không check Manager assignment. */
    @Transactional(readOnly = true)
    public BookingTrendReportResponse getBookingTrendReportAdmin(UUID propertyId,
                                                                 LocalDate from, LocalDate to, String groupBy) {
        return computeBookingTrend(propertyId, from, to, groupBy);
    }

    private BookingTrendReportResponse computeBookingTrend(UUID propertyId,
                                                           LocalDate from, LocalDate to, String groupBy) {
        boolean isWeek = "week".equalsIgnoreCase(groupBy);
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(23, 59, 59, 999_999_999);

        LinkedHashMap<String, long[]> buckets = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            buckets.computeIfAbsent(periodKey(d, isWeek), k -> new long[1]);
        }

        List<LocalDateTime> createdTimes = bookingRepository.findBookingCreationTimes(
                propertyId, COUNTED_STATUSES, fromDt, toDt);
        for (LocalDateTime createdAt : createdTimes) {
            long[] bucket = buckets.get(periodKey(createdAt.toLocalDate(), isWeek));
            if (bucket != null) bucket[0] += 1;
        }

        List<PeriodBookingCount> byPeriod = buckets.entrySet().stream()
                .map(e -> PeriodBookingCount.builder()
                        .period(e.getKey())
                        .bookingCount(e.getValue()[0])
                        .build())
                .toList();

        List<StatusCount> byStatus = bookingRepository
                .countByStatusForPropertyInRange(propertyId, fromDt, toDt).stream()
                .map(row -> StatusCount.builder()
                        .status(((Booking.Status) row[0]).name())
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();

        long total = byPeriod.stream().mapToLong(PeriodBookingCount::getBookingCount).sum();

        return BookingTrendReportResponse.builder()
                .totalBookings(total)
                .byPeriod(byPeriod)
                .byStatus(byStatus)
                .build();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    /** "2026-01" (month) hoặc "2026-W22" (week) — lexicographic = chronological. */
    private String periodKey(LocalDate date, boolean isWeek) {
        if (isWeek) {
            int year = date.get(WeekFields.ISO.weekBasedYear());
            int week = date.get(WeekFields.ISO.weekOfWeekBasedYear());
            return String.format("%d-W%02d", year, week);
        }
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    /** Tỷ lệ % làm tròn 1 chữ số thập phân. */
    private double rate(long part, long total) {
        if (total == 0) return 0.0;
        return BigDecimal.valueOf(part * 100.0 / total)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
