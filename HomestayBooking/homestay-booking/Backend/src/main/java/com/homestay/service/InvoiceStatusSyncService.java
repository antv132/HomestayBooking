package com.homestay.service;

import com.homestay.entity.Invoice;
import com.homestay.entity.Payment;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/** FR-11 — sync invoice status from FR-12 payment lifecycle. */
@Service
public class InvoiceStatusSyncService {

    private final InvoiceRepository invoiceRepository;
    private final ActivityLogService activityLogService;

    public InvoiceStatusSyncService(InvoiceRepository invoiceRepository,
                                    ActivityLogService activityLogService) {
        this.invoiceRepository = invoiceRepository;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public void markPendingPayment(UUID invoiceId) {
        Invoice invoice = find(invoiceId);
        if (invoice.getStatus() == Invoice.Status.PAID || invoice.getStatus() == Invoice.Status.CANCELLED) {
            return;
        }
        invoice.setStatus(Invoice.Status.PENDING_PAYMENT);
        invoiceRepository.save(invoice);
    }

    @Transactional
    public void markPaid(UUID invoiceId, LocalDateTime paidAt) {
        Invoice invoice = find(invoiceId);
        if (invoice.getStatus() == Invoice.Status.PAID) {
            return;
        }
        if (invoice.getStatus() == Invoice.Status.CANCELLED) {
            throw new BusinessException("Không thể đánh dấu Paid cho invoice đã Cancelled");
        }
        invoice.setStatus(Invoice.Status.PAID);
        invoice.setPaidAt(paidAt != null ? paidAt : LocalDateTime.now());
        invoiceRepository.save(invoice);
        activityLogService.log(
                invoice.getCustomer(),
                ActivityLogService.INVOICE_PAID,
                "Invoice",
                invoice.getId(),
                "Invoice " + invoice.getType() + " paid amount=" + invoice.getAmount());
    }

    @Transactional
    public void markFailed(UUID invoiceId) {
        Invoice invoice = find(invoiceId);
        if (invoice.getStatus() == Invoice.Status.PENDING_PAYMENT) {
            invoice.setStatus(Invoice.Status.UNPAID);
            invoiceRepository.save(invoice);
        }
    }

    /**
     * FR-12 entry point when Payment status changes.
     */
    @Transactional
    public void syncFromPayment(UUID invoiceId, Payment.Status paymentStatus, LocalDateTime paidAt) {
        if (invoiceId == null || paymentStatus == null) {
            return;
        }
        switch (paymentStatus) {
            case PENDING -> markPendingPayment(invoiceId);
            case PAID -> markPaid(invoiceId, paidAt);
            case FAILED -> markFailed(invoiceId);
            default -> {
                // REFUNDED / PARTIALLY_REFUNDED — leave invoice PAID (refund is FR-12)
            }
        }
    }

    @Transactional
    public void syncFromPaymentEntity(Payment payment) {
        if (payment == null || payment.getInvoice() == null) {
            // Fallback: match by booking + type when invoice_id not set yet
            if (payment == null || payment.getBooking() == null) {
                return;
            }
            Invoice.Type invType = mapPaymentType(payment.getType());
            if (invType == null) {
                return;
            }
            invoiceRepository.findByBooking_IdAndType(payment.getBooking().getId(), invType)
                    .ifPresent(inv -> {
                        payment.setInvoice(inv);
                        syncFromPayment(inv.getId(), payment.getStatus(), payment.getPaidAt());
                    });
            return;
        }
        syncFromPayment(payment.getInvoice().getId(), payment.getStatus(), payment.getPaidAt());
    }

    private static Invoice.Type mapPaymentType(Payment.Type type) {
        if (type == Payment.Type.DEPOSIT) {
            return Invoice.Type.DEPOSIT;
        }
        if (type == Payment.Type.REMAINING_BALANCE) {
            return Invoice.Type.REMAINING_BALANCE;
        }
        return null;
    }

    private Invoice find(UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice không tồn tại"));
    }
}
