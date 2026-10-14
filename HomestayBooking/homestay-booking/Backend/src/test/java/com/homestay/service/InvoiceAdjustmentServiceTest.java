package com.homestay.service;

import com.homestay.entity.Booking;
import com.homestay.entity.Invoice;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceAdjustmentServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock ActivityLogService activityLogService;

    private InvoiceAdjustmentService service;
    private Booking booking;
    private Invoice deposit;
    private Invoice remaining;

    @BeforeEach
    void setUp() {
        service = new InvoiceAdjustmentService(invoiceRepository, activityLogService);
        User customer = TestFixtures.user(User.Role.CUSTOMER);
        Property property = TestFixtures.property();
        Room room = TestFixtures.room(property);
        booking = TestFixtures.booking(customer, room, Booking.Status.CONFIRMED);
        booking.setId(UUID.randomUUID());
        booking.setDepositAmount(new BigDecimal("400000"));
        booking.setRemainingAmount(new BigDecimal("900000"));

        deposit = new Invoice();
        deposit.setId(UUID.randomUUID());
        deposit.setBooking(booking);
        deposit.setCustomer(customer);
        deposit.setType(Invoice.Type.DEPOSIT);
        deposit.setAmount(new BigDecimal("400000"));
        deposit.setStatus(Invoice.Status.PAID);

        remaining = new Invoice();
        remaining.setId(UUID.randomUUID());
        remaining.setBooking(booking);
        remaining.setCustomer(customer);
        remaining.setType(Invoice.Type.REMAINING_BALANCE);
        remaining.setAmount(new BigDecimal("600000"));
        remaining.setStatus(Invoice.Status.UNPAID);
    }

    @Test
    void adjust_whenDepositPaid_updatesRemainingOnly() {
        when(invoiceRepository.findByBooking_IdAndType(booking.getId(), Invoice.Type.DEPOSIT))
                .thenReturn(Optional.of(deposit));
        when(invoiceRepository.findByBooking_IdAndType(booking.getId(), Invoice.Type.REMAINING_BALANCE))
                .thenReturn(Optional.of(remaining));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.adjustOnBookingModify(booking);

        assertEquals(0, new BigDecimal("400000").compareTo(deposit.getAmount()));
        assertEquals(0, new BigDecimal("900000").compareTo(remaining.getAmount()));
        verify(activityLogService).log(any(), org.mockito.ArgumentMatchers.eq(ActivityLogService.INVOICE_ADJUSTED), any(), any(), any());
    }
}
