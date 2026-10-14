package com.homestay.service;

import com.homestay.entity.Booking;
import com.homestay.entity.Invoice;
import com.homestay.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** FR-11 — adjust invoice amounts when Confirmed booking is modified. */
@Service
public class InvoiceAdjustmentService {

    private final InvoiceRepository invoiceRepository;
    private final ActivityLogService activityLogService;

    public InvoiceAdjustmentService(InvoiceRepository invoiceRepository,
                                    ActivityLogService activityLogService) {
        this.invoiceRepository = invoiceRepository;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public void adjustOnBookingModify(Booking booking) {
        Invoice deposit = invoiceRepository
                .findByBooking_IdAndType(booking.getId(), Invoice.Type.DEPOSIT)
                .orElse(null);
        Invoice remaining = invoiceRepository
                .findByBooking_IdAndType(booking.getId(), Invoice.Type.REMAINING_BALANCE)
                .orElse(null);

        boolean depositPaid = deposit != null && deposit.getStatus() == Invoice.Status.PAID;
        BigDecimal newDeposit = nz(booking.getDepositAmount());
        BigDecimal newRemaining = nz(booking.getRemainingAmount());

        if (!depositPaid) {
            // Both unpaid (or deposit unpaid): update both amounts when total changed
            if (deposit != null && isOpen(deposit) && deposit.getAmount().compareTo(newDeposit) != 0) {
                deposit.setAmount(newDeposit);
                invoiceRepository.save(deposit);
                logAdjusted(deposit, "deposit amount → " + newDeposit);
            }
            if (remaining != null && isOpen(remaining) && remaining.getAmount().compareTo(newRemaining) != 0) {
                remaining.setAmount(newRemaining);
                if (booking.getCheckInDate() != null) {
                    remaining.setDueDate(booking.getCheckInDate());
                }
                invoiceRepository.save(remaining);
                logAdjusted(remaining, "remaining amount → " + newRemaining);
            }
            return;
        }

        // Deposit PAID immutable — only remaining (if unpaid) adjusts
        if (remaining != null && isOpen(remaining) && remaining.getAmount().compareTo(newRemaining) != 0) {
            remaining.setAmount(newRemaining);
            if (booking.getCheckInDate() != null) {
                remaining.setDueDate(booking.getCheckInDate());
            } else {
                remaining.setDueDate(LocalDate.now());
            }
            invoiceRepository.save(remaining);
            logAdjusted(remaining, "remaining amount → " + newRemaining + " (deposit paid immutable)");
        }
    }

    private static boolean isOpen(Invoice invoice) {
        return invoice.getStatus() == Invoice.Status.UNPAID
                || invoice.getStatus() == Invoice.Status.PENDING_PAYMENT;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private void logAdjusted(Invoice invoice, String detail) {
        activityLogService.log(
                invoice.getCustomer(),
                ActivityLogService.INVOICE_ADJUSTED,
                "Invoice",
                invoice.getId(),
                detail);
    }
}
