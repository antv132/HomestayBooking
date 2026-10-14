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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceIssuanceServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock ActivityLogService activityLogService;

    private InvoiceIssuanceService service;
    private Booking booking;

    @BeforeEach
    void setUp() {
        service = new InvoiceIssuanceService(invoiceRepository, activityLogService);
        User customer = TestFixtures.user(User.Role.CUSTOMER);
        Property property = TestFixtures.property();
        Room room = TestFixtures.room(property);
        booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_DEPOSIT);
        booking.setId(UUID.randomUUID());
        booking.setTotalAmount(new BigDecimal("1000000"));
        booking.setDepositAmount(new BigDecimal("400000"));
        booking.setRemainingAmount(new BigDecimal("600000"));
        booking.setCheckInDate(LocalDate.now().plusDays(7));
    }

    @Test
    void issuePair_createsDepositAndRemaining_idempotent() {
        when(invoiceRepository.existsByBooking_IdAndType(booking.getId(), Invoice.Type.DEPOSIT)).thenReturn(false);
        when(invoiceRepository.existsByBooking_IdAndType(booking.getId(), Invoice.Type.REMAINING_BALANCE)).thenReturn(false);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) {
                i.setId(UUID.randomUUID());
            }
            return i;
        });

        List<Invoice> issued = service.issuePair(booking);
        assertEquals(2, issued.size());
        verify(invoiceRepository, times(2)).save(any(Invoice.class));
        verify(activityLogService).log(any(), org.mockito.ArgumentMatchers.eq(ActivityLogService.INVOICE_ISSUED), any(), any(), any());

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository, times(2)).save(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(i ->
                i.getType() == Invoice.Type.DEPOSIT && i.getAmount().compareTo(new BigDecimal("400000")) == 0));
        assertTrue(captor.getAllValues().stream().anyMatch(i ->
                i.getType() == Invoice.Type.REMAINING_BALANCE
                        && i.getAmount().compareTo(new BigDecimal("600000")) == 0
                        && i.getDueDate().equals(booking.getCheckInDate())));
    }

    @Test
    void issuePair_skipsWhenAlreadyExists() {
        when(invoiceRepository.existsByBooking_IdAndType(booking.getId(), Invoice.Type.DEPOSIT)).thenReturn(true);
        when(invoiceRepository.existsByBooking_IdAndType(booking.getId(), Invoice.Type.REMAINING_BALANCE)).thenReturn(true);

        List<Invoice> issued = service.issuePair(booking);
        assertTrue(issued.isEmpty());
        verify(invoiceRepository, never()).save(any());
    }
}
