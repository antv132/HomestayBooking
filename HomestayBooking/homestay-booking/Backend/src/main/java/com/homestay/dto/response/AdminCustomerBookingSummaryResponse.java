package com.homestay.dto.response;

import com.homestay.entity.Booking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** SCR-51 — Tóm tắt booking trong Drawer Customer Directory. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCustomerBookingSummaryResponse {

    private UUID id;
    private String roomNumber;
    private String propertyName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String status;
    private BigDecimal totalAmount;

    public static AdminCustomerBookingSummaryResponse fromEntity(Booking b) {
        return AdminCustomerBookingSummaryResponse.builder()
                .id(b.getId())
                .roomNumber(b.getRoom().getRoomNumber())
                .propertyName(b.getRoom().getProperty().getName())
                .checkInDate(b.getCheckInDate())
                .checkOutDate(b.getCheckOutDate())
                .status(b.getStatus().name())
                .totalAmount(b.getTotalAmount())
                .build();
    }
}
