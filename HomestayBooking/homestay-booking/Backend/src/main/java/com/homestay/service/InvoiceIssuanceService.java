package com.homestay.service;

import com.homestay.entity.Booking;
import com.homestay.entity.Invoice;
import com.homestay.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** FR-11 — issue deposit + remaining invoice pair on booking create. */
@Service
public class InvoiceIssuanceService {

    private final InvoiceRepository invoiceRepository;
    private final ActivityLogService activityLogService;

    public InvoiceIssuanceService(InvoiceRepository invoiceRepository,
                                  ActivityLogService activityLogService) {
        this.invoiceRepository = invoiceRepository;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public List<Invoice> issuePair(Booking booking) {
        List<Invoice> issued = new ArrayList<>();
        BigDecimal deposit = booking.getDepositAmount() != null ? booking.getDepositAmount() : BigDecimal.ZERO;
        BigDecimal remaining = booking.getRemainingAmount() != null ? booking.getRemainingAmount() : BigDecimal.ZERO;

        if (!invoiceRepository.existsByBooking_IdAndType(booking.getId(), Invoice.Type.DEPOSIT)) {
            Invoice depositInv = newInvoice(booking, Invoice.Type.DEPOSIT, deposit, LocalDate.now());
            issued.add(invoiceRepository.save(depositInv));
        }
        if (!invoiceRepository.existsByBooking_IdAndType(booking.getId(), Invoice.Type.REMAINING_BALANCE)) {
            LocalDate due = booking.getCheckInDate() != null ? booking.getCheckInDate() : LocalDate.now();
            Invoice remainingInv = newInvoice(booking, Invoice.Type.REMAINING_BALANCE, remaining, due);
            issued.add(invoiceRepository.save(remainingInv));
        }

        if (!issued.isEmpty()) {
            activityLogService.log(
                    booking.getCustomer(),
                    ActivityLogService.INVOICE_ISSUED,
                    "Invoice",
                    issued.get(0).getId(),
                    "Issued invoice pair for booking " + booking.getId()
                            + " deposit=" + deposit + " remaining=" + remaining);
        }
        return issued;
    }

    private static Invoice newInvoice(Booking booking, Invoice.Type type, BigDecimal amount, LocalDate dueDate) {
        Invoice inv = new Invoice();
        inv.setBooking(booking);
        inv.setCustomer(booking.getCustomer());
        inv.setProperty(booking.getRoom().getProperty());
        inv.setType(type);
        inv.setAmount(amount);
        inv.setStatus(Invoice.Status.UNPAID);
        inv.setDueDate(dueDate);
        return inv;
    }
}
