package com.homestay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** SCR-15 — Customer Dashboard summary */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDashboardResponse {

    private int activeBookings;
    private int pendingPayments;
    private int openTickets;
    private int unreadNotifications;

    private UpcomingEvent upcomingCheckIn;
    private UpcomingEvent upcomingCheckOut;

    private List<BookingSummaryResponse> upcomingBookings;
    private List<PaymentSummary> recentPayments;
    private List<NotificationSummary> recentNotifications;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpcomingEvent {
        private UUID bookingId;
        private String roomNumber;
        private String propertyName;
        private LocalDate date;
        private long daysUntil;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSummary {
        private UUID id;
        private String type;
        private BigDecimal amount;
        private String status;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationSummary {
        private UUID id;
        private String title;
        private String content;
        private String type;
        private boolean isRead;
        private LocalDateTime createdAt;
    }
}
