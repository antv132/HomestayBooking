package com.homestay.service;

import com.homestay.dto.response.RevenueReportResponse;
import com.homestay.dto.response.RevenueReportResponse.PeriodRevenue;
import com.homestay.dto.response.RevenueReportResponse.PropertyRevenue;
import com.homestay.entity.Payment;
import com.homestay.entity.Property;
import com.homestay.entity.User;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;

/**
 * Service xử lý các báo cáo thống kê doanh thu (SCR-59 / FR-16).
 * Manager path luôn property-scoped (FR-011); Admin path có thể null = toàn hệ thống.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final PaymentRepository paymentRepository;
    private final ReportPropertyScopeValidator scopeValidator;
    private final ManagerPropertyAssignmentRepository assignmentRepository;

    // ── SCR-59: Revenue Report ──────────────────────────────────────────────────

    /**
     * Tổng hợp báo cáo doanh thu theo kỳ (Admin / internal — không check Manager assignment).
     *
     * @param propertyId UUID của property, null = tất cả
     * @param from       Ngày bắt đầu (bao gồm), null = không giới hạn
     * @param to         Ngày kết thúc (bao gồm), null = không giới hạn
     * @param groupBy    "month" (default) hoặc "week"
     */
    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport(UUID propertyId,
                                                   LocalDate from,
                                                   LocalDate to,
                                                   String groupBy) {
        LocalDateTime fromDt = (from != null) ? from.atStartOfDay() : null;
        LocalDateTime toDt = (to != null) ? to.atTime(23, 59, 59, 999_999_999) : null;

        BigDecimal totalRevenue = paymentRepository.sumRevenueByType(fromDt, toDt, propertyId, null);
        BigDecimal depositRevenue = paymentRepository.sumRevenueByType(fromDt, toDt, propertyId, Payment.Type.DEPOSIT);
        BigDecimal balanceRevenue = paymentRepository.sumRevenueByType(fromDt, toDt, propertyId, Payment.Type.REMAINING_BALANCE);
        long totalBookings = paymentRepository.countDistinctBookings(fromDt, toDt, propertyId);
        List<Object[]> raw = paymentRepository.findRevenueRawData(fromDt, toDt, propertyId);

        return buildResponse(totalRevenue, depositRevenue, balanceRevenue, totalBookings, raw, groupBy);
    }

    /**
     * Manager revenue — FR-011: chỉ property được gán.
     * {@code propertyId} null → gộp mọi property ACTIVE assignment của manager.
     */
    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReportForManager(User manager,
                                                            UUID propertyId,
                                                            LocalDate from,
                                                            LocalDate to,
                                                            String groupBy) {
        if (propertyId != null) {
            scopeValidator.validateManagerAccess(manager, propertyId);
            return getRevenueReport(propertyId, from, to, groupBy);
        }

        List<UUID> assignedIds = assignmentRepository.findAssignedPropertiesByManagerId(manager.getId())
                .stream()
                .map(Property::getId)
                .toList();
        if (assignedIds.isEmpty()) {
            return emptyReport();
        }
        if (assignedIds.size() == 1) {
            return getRevenueReport(assignedIds.get(0), from, to, groupBy);
        }
        return getRevenueReportForProperties(assignedIds, from, to, groupBy);
    }

    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReportForProperties(List<UUID> propertyIds,
                                                               LocalDate from,
                                                               LocalDate to,
                                                               String groupBy) {
        if (propertyIds == null || propertyIds.isEmpty()) {
            return emptyReport();
        }
        LocalDateTime fromDt = (from != null) ? from.atStartOfDay() : null;
        LocalDateTime toDt = (to != null) ? to.atTime(23, 59, 59, 999_999_999) : null;

        BigDecimal totalRevenue = paymentRepository.sumRevenueByTypeForProperties(fromDt, toDt, propertyIds, null);
        BigDecimal depositRevenue = paymentRepository.sumRevenueByTypeForProperties(
                fromDt, toDt, propertyIds, Payment.Type.DEPOSIT);
        BigDecimal balanceRevenue = paymentRepository.sumRevenueByTypeForProperties(
                fromDt, toDt, propertyIds, Payment.Type.REMAINING_BALANCE);
        long totalBookings = paymentRepository.countDistinctBookingsForProperties(fromDt, toDt, propertyIds);
        List<Object[]> raw = paymentRepository.findRevenueRawDataForProperties(fromDt, toDt, propertyIds);

        return buildResponse(totalRevenue, depositRevenue, balanceRevenue, totalBookings, raw, groupBy);
    }

    private RevenueReportResponse buildResponse(BigDecimal totalRevenue,
                                                BigDecimal depositRevenue,
                                                BigDecimal balanceRevenue,
                                                long totalBookings,
                                                List<Object[]> raw,
                                                String groupBy) {
        return RevenueReportResponse.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .depositRevenue(depositRevenue != null ? depositRevenue : BigDecimal.ZERO)
                .balanceRevenue(balanceRevenue != null ? balanceRevenue : BigDecimal.ZERO)
                .totalBookingCount(totalBookings)
                .byPeriod(groupByPeriod(raw, groupBy))
                .byProperty(groupByProperty(raw))
                .build();
    }

    private RevenueReportResponse emptyReport() {
        return RevenueReportResponse.builder()
                .totalRevenue(BigDecimal.ZERO)
                .depositRevenue(BigDecimal.ZERO)
                .balanceRevenue(BigDecimal.ZERO)
                .totalBookingCount(0)
                .byPeriod(List.of())
                .byProperty(List.of())
                .build();
    }

    // ── Private helpers ─────────────────────────────────────────────────────────

    private List<PeriodRevenue> groupByPeriod(List<Object[]> raw, String groupBy) {
        boolean isWeek = "week".equalsIgnoreCase(groupBy);
        LinkedHashMap<String, PeriodAccum> map = new LinkedHashMap<>();

        for (Object[] row : raw) {
            LocalDateTime paidAt = (LocalDateTime) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            UUID bookingId = (UUID) row[5];
            String key = isWeek ? toWeekKey(paidAt) : toMonthKey(paidAt);
            map.computeIfAbsent(key, k -> new PeriodAccum()).add(amount, bookingId);
        }

        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> PeriodRevenue.builder()
                        .period(e.getKey())
                        .revenue(e.getValue().revenue)
                        .bookingCount(e.getValue().bookingIds.size())
                        .build())
                .toList();
    }

    private List<PropertyRevenue> groupByProperty(List<Object[]> raw) {
        LinkedHashMap<String, PropAccum> map = new LinkedHashMap<>();

        for (Object[] row : raw) {
            BigDecimal amount = (BigDecimal) row[1];
            UUID propId = (UUID) row[3];
            String propName = (String) row[4];
            UUID bookingId = (UUID) row[5];
            String key = propId.toString();
            map.computeIfAbsent(key, k -> new PropAccum(propId, propName)).add(amount, bookingId);
        }

        return map.values().stream()
                .sorted((a, b) -> b.revenue.compareTo(a.revenue))
                .map(p -> PropertyRevenue.builder()
                        .propertyId(p.propertyId.toString())
                        .propertyName(p.propertyName)
                        .revenue(p.revenue)
                        .bookingCount(p.bookingIds.size())
                        .build())
                .toList();
    }

    private String toMonthKey(LocalDateTime dt) {
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    private String toWeekKey(LocalDateTime dt) {
        int year = dt.getYear();
        int week = dt.get(WeekFields.ISO.weekOfWeekBasedYear());
        return String.format("%d-W%02d", year, week);
    }

    private static class PeriodAccum {
        BigDecimal revenue = BigDecimal.ZERO;
        Set<UUID> bookingIds = new LinkedHashSet<>();

        void add(BigDecimal amount, UUID bookingId) {
            revenue = revenue.add(amount);
            bookingIds.add(bookingId);
        }
    }

    private static class PropAccum {
        final UUID propertyId;
        final String propertyName;
        BigDecimal revenue = BigDecimal.ZERO;
        Set<UUID> bookingIds = new LinkedHashSet<>();

        PropAccum(UUID propertyId, String propertyName) {
            this.propertyId = propertyId;
            this.propertyName = propertyName;
        }

        void add(BigDecimal amount, UUID bookingId) {
            revenue = revenue.add(amount);
            bookingIds.add(bookingId);
        }
    }
}
