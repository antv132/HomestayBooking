package com.homestay.dto.response;

import com.homestay.entity.Contract;
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
public class ContractDetailResponse {
    private UUID id;
    private UUID bookingId;
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String roomNumber;
    private String propertyName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal depositAmount;
    private BigDecimal totalAmount;
    private String pdfUrl;
    private LocalDateTime generatedAt;
    private LocalDateTime sentAt;
    private String status;

    public static ContractDetailResponse fromEntity(Contract c) {
        String storedPdf = c.getPdfUrl();
        String pdfUrl = (storedPdf != null && !storedPdf.isBlank())
                ? storedPdf
                : "/api/v1/contracts/" + c.getId() + "/pdf";
        return new ContractDetailResponse(
                c.getId(),
                c.getBooking().getId(),
                c.getCustomer().getId(),
                c.getCustomer().getFullName() != null ? c.getCustomer().getFullName() : "Unknown",
                c.getCustomer().getEmail(),
                c.getCustomer().getPhone(),
                c.getRoom().getRoomNumber(),
                c.getRoom().getProperty().getName(),
                c.getCheckInDate(),
                c.getCheckOutDate(),
                c.getDepositAmount(),
                c.getTotalAmount(),
                pdfUrl,
                c.getGeneratedAt(),
                c.getSentAt(),
                c.getStatus().name()
        );
    }
}
