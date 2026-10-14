package com.homestay.service;

import com.homestay.dto.response.InvoiceBreakdownResponse;
import com.homestay.dto.response.InvoiceDetailResponse;
import com.homestay.dto.response.InvoiceSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.Invoice;
import com.homestay.entity.User;
import com.homestay.exception.ForbiddenException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.InvoiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** FR-11 — customer/manager invoice queries and booking breakdown. */
@Service
public class InvoiceQueryService {

    private final InvoiceRepository invoiceRepository;
    private final BookingRepository bookingRepository;
    private final ReportPropertyScopeValidator scopeValidator;

    public InvoiceQueryService(InvoiceRepository invoiceRepository,
                               BookingRepository bookingRepository,
                               ReportPropertyScopeValidator scopeValidator) {
        this.invoiceRepository = invoiceRepository;
        this.bookingRepository = bookingRepository;
        this.scopeValidator = scopeValidator;
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceSummaryResponse> listForCustomer(
            UUID customerId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Invoice> result;
        if (status != null && !status.isBlank()) {
            Invoice.Status st = Invoice.Status.valueOf(status.trim().toUpperCase());
            result = invoiceRepository.findByCustomer_IdAndStatus(customerId, st, pageable);
        } else {
            result = invoiceRepository.findByCustomer_Id(customerId, pageable);
        }
        return toPage(result);
    }

    @Transactional(readOnly = true)
    public InvoiceDetailResponse getDetailForCustomer(UUID invoiceId, UUID customerId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice không tồn tại"));
        if (!invoice.getCustomer().getId().equals(customerId)) {
            throw new ForbiddenException("Không có quyền xem hóa đơn này");
        }
        return toDetail(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceBreakdownResponse buildBreakdownForBooking(UUID bookingId, UUID customerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking không tồn tại"));
        if (!booking.getCustomer().getId().equals(customerId)) {
            throw new ForbiddenException("Không có quyền xem booking này");
        }
        return buildBreakdown(booking);
    }

    @Transactional(readOnly = true)
    public InvoiceBreakdownResponse buildBreakdownForManagerBooking(UUID bookingId, User manager) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking không tồn tại"));
        scopeValidator.validateManagerAccess(manager, booking.getRoom().getProperty().getId());
        return buildBreakdown(booking);
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceSummaryResponse> listForManager(
            User manager, UUID propertyId, String status, int page, int size) {
        if (propertyId == null) {
            throw new IllegalArgumentException("propertyId là bắt buộc");
        }
        scopeValidator.validateManagerAccess(manager, propertyId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Invoice.Status st = null;
        if (status != null && !status.isBlank()) {
            st = Invoice.Status.valueOf(status.trim().toUpperCase());
        }
        Page<Invoice> result = invoiceRepository.findByPropertyIdAndOptionalStatus(propertyId, st, pageable);
        return toPage(result);
    }

    public InvoiceBreakdownResponse buildBreakdown(Booking booking) {
        List<Invoice> invoices = invoiceRepository.findByBooking_IdOrderByTypeAsc(booking.getId());
        InvoiceBreakdownResponse breakdown = new InvoiceBreakdownResponse();
        breakdown.setTotalAmount(nz(booking.getTotalAmount()));
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal remainingDue = BigDecimal.ZERO;
        for (Invoice inv : invoices) {
            breakdown.getInvoices().add(InvoiceSummaryResponse.fromEntity(inv));
            if (inv.getStatus() == Invoice.Status.PAID) {
                paid = paid.add(nz(inv.getAmount()));
            } else if (inv.getStatus() == Invoice.Status.UNPAID
                    || inv.getStatus() == Invoice.Status.PENDING_PAYMENT) {
                remainingDue = remainingDue.add(nz(inv.getAmount()));
            }
        }
        breakdown.setPaidAmount(paid);
        breakdown.setRemainingBalance(remainingDue);
        return breakdown;
    }

    private static InvoiceDetailResponse toDetail(Invoice invoice) {
        InvoiceDetailResponse detail = new InvoiceDetailResponse();
        detail.setInvoice(InvoiceSummaryResponse.fromEntity(invoice));
        detail.setPropertyName(invoice.getProperty().getName());
        detail.setRoomNumber(invoice.getBooking().getRoom().getRoomNumber());
        return detail;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static PageResponse<InvoiceSummaryResponse> toPage(Page<Invoice> result) {
        return new PageResponse<>(
                result.map(InvoiceSummaryResponse::fromEntity).getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }
}
