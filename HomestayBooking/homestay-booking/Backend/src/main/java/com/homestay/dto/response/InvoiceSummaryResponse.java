package com.homestay.dto.response;

import com.homestay.entity.Invoice;
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
public class InvoiceSummaryResponse {
    private UUID id;
    private UUID bookingId;
    private String type;
    private BigDecimal amount;
    private String status;
    private LocalDate dueDate;
    private LocalDateTime paidAt;
    private boolean overdue;
    private LocalDateTime createdAt;

    public static InvoiceSummaryResponse fromEntity(Invoice invoice) {
        boolean overdue = invoice.getStatus() != Invoice.Status.PAID
                && invoice.getStatus() != Invoice.Status.CANCELLED
                && invoice.getDueDate() != null
                && invoice.getDueDate().isBefore(LocalDate.now());
        return new InvoiceSummaryResponse(
                invoice.getId(),
                invoice.getBooking().getId(),
                invoice.getType().name(),
                invoice.getAmount(),
                invoice.getStatus().name(),
                invoice.getDueDate(),
                invoice.getPaidAt(),
                overdue,
                invoice.getCreatedAt()
        );
    }
}
