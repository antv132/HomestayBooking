package com.homestay.service;

import com.homestay.entity.Invoice;
import com.homestay.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** FR-11 — cancel unpaid/pending invoices when booking cancelled. */
@Service
public class InvoiceCancellationService {

    private final InvoiceRepository invoiceRepository;
    private final ActivityLogService activityLogService;

    public InvoiceCancellationService(InvoiceRepository invoiceRepository,
                                      ActivityLogService activityLogService) {
        this.invoiceRepository = invoiceRepository;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public int cancelUnpaidForBooking(UUID bookingId) {
        List<Invoice> open = invoiceRepository.findByBooking_IdAndStatusIn(
                bookingId,
                List.of(Invoice.Status.UNPAID, Invoice.Status.PENDING_PAYMENT));
        int count = 0;
        for (Invoice invoice : open) {
            invoice.setStatus(Invoice.Status.CANCELLED);
            invoiceRepository.save(invoice);
            count++;
            activityLogService.log(
                    invoice.getCustomer(),
                    ActivityLogService.INVOICE_CANCELLED,
                    "Invoice",
                    invoice.getId(),
                    "Cancelled " + invoice.getType() + " for booking " + bookingId);
        }
        return count;
    }
}
