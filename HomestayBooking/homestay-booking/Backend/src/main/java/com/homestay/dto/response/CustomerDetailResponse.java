package com.homestay.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CustomerDetailResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private String status;
    private LocalDateTime createdAt;
    private long bookingCount;
    private List<BookingSummary> recentBookings;

    @Data
    @Builder
    public static class BookingSummary {
        private UUID id;
        private String roomNumber;
        private String propertyName;
        private String checkInDate;
        private String checkOutDate;
        private long totalAmount;
        private String status;
    }
}
