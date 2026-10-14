package com.homestay.service;

import com.homestay.entity.Booking;
import com.homestay.entity.Invoice;
import com.homestay.entity.Payment;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.repository.InvoiceRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceStatusSyncServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock ActivityLogService activityLogService;

    private InvoiceStatusSyncService service;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        service = new InvoiceStatusSyncService(invoiceRepository, activityLogService);
        User customer = TestFixtures.user(User.Role.CUSTOMER);
        Property property = TestFixtures.property();
        Room room = TestFixtures.room(property);
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_DEPOSIT);
        invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setBooking(booking);
        invoice.setCustomer(customer);
        invoice.setProperty(property);
        invoice.setType(Invoice.Type.DEPOSIT);
        invoice.setAmount(new BigDecimal("400000"));
        invoice.setStatus(Invoice.Status.UNPAID);
    }

    @Test
    void markPendingPayment_fromUnpaid() {
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markPendingPayment(invoice.getId());
        assertEquals(Invoice.Status.PENDING_PAYMENT, invoice.getStatus());
    }

    @Test
    void markPaid_setsPaidAtAndLogs() {
        invoice.setStatus(Invoice.Status.PENDING_PAYMENT);
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime paidAt = LocalDateTime.now();
        service.markPaid(invoice.getId(), paidAt);
        assertEquals(Invoice.Status.PAID, invoice.getStatus());
        assertEquals(paidAt, invoice.getPaidAt());
        verify(activityLogService).log(any(), org.mockito.ArgumentMatchers.eq(ActivityLogService.INVOICE_PAID), any(), any(), any());
    }

    @Test
    void syncFromPayment_failedResetsToUnpaid() {
        invoice.setStatus(Invoice.Status.PENDING_PAYMENT);
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.syncFromPayment(invoice.getId(), Payment.Status.FAILED, null);
        assertEquals(Invoice.Status.UNPAID, invoice.getStatus());
    }
}
