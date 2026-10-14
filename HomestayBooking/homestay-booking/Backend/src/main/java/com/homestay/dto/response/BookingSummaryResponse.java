package com.homestay.dto.response;

import com.homestay.entity.Booking;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingSummaryResponse {

    private UUID id;
    private String customerName;
    private String customerEmail;
    private String roomNumber;
    private String roomType;
    private String propertyName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guestCount;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;
    private boolean isReviewed;

    public static BookingSummaryResponse fromEntity(Booking booking) {
        return fromEntity(booking, false);
    }

    public static BookingSummaryResponse fromEntity(Booking booking, boolean isReviewed) {
        return new BookingSummaryResponse(
                booking.getId(),
                booking.getCustomer().getFullName(),
                booking.getCustomer().getEmail(),
                booking.getRoom().getRoomNumber(),
                booking.getRoom().getRoomType(),
                booking.getRoom().getProperty().getName(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getGuestCount(),
                booking.getTotalAmount(),
                booking.getStatus().name(),
                booking.getCreatedAt(),
                isReviewed
        );
    }
}
