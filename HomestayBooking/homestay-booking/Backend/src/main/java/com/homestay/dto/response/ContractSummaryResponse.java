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
public class ContractSummaryResponse {
    private UUID id;
    private UUID bookingId;
    private String customerName;
    private String customerEmail;
    /** SCR-21 api-spec field (alias of room number / display name). */
    private String roomName;
    private String roomNumber;
    private String propertyName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal depositAmount;
    private BigDecimal totalAmount;
    /** Absolute or API-relative PDF location (SCR-21). */
    private String pdfUrl;
    private String status;
    private LocalDateTime generatedAt;
    private LocalDateTime sentAt;

    public static ContractSummaryResponse fromEntity(Contract c) {
        String roomNumber = c.getRoom().getRoomNumber();
        String storedPdf = c.getPdfUrl();
        String pdfUrl = (storedPdf != null && !storedPdf.isBlank())
                ? storedPdf
                : "/api/v1/contracts/" + c.getId() + "/pdf";

        return new ContractSummaryResponse(
                c.getId(),
                c.getBooking().getId(),
                c.getCustomer().getFullName() != null ? c.getCustomer().getFullName() : "Unknown",
                c.getCustomer().getEmail(),
                roomNumber,
                roomNumber,
                c.getRoom().getProperty().getName(),
                c.getCheckInDate(),
                c.getCheckOutDate(),
                c.getDepositAmount(),
                c.getTotalAmount(),
                pdfUrl,
                c.getStatus().name(),
                c.getGeneratedAt(),
                c.getSentAt()
        );
    }
}

