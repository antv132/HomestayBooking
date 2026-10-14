package com.homestay.service;

import com.homestay.dto.response.InvoiceBreakdownResponse;
import com.homestay.dto.response.InvoiceDetailResponse;
import com.homestay.dto.response.InvoiceSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.Invoice;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.ForbiddenException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.InvoiceRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceQueryServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock BookingRepository bookingRepository;
    @Mock ReportPropertyScopeValidator scopeValidator;

    private InvoiceQueryService invoiceQueryService;
    private User customer;
    private User manager;
    private Property property;
    private Room room;
    private Booking booking;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        invoiceQueryService = new InvoiceQueryService(
                invoiceRepository,
                bookingRepository,
                scopeValidator);

        customer = TestFixtures.user(User.Role.CUSTOMER);
        manager = TestFixtures.user(User.Role.MANAGER);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
        booking = TestFixtures.booking(customer, room, Booking.Status.CONFIRMED);

        invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setBooking(booking);
        invoice.setCustomer(customer);
        invoice.setProperty(property);
        invoice.setType(Invoice.Type.DEPOSIT);
        invoice.setStatus(Invoice.Status.PAID);
        invoice.setAmount(booking.getDepositAmount());
    }

    @Test
    void listForCustomer_returnsCustomerInvoices() {
        Page<Invoice> page = new PageImpl<>(List.of(invoice));
        when(invoiceRepository.findByCustomer_Id(eq(customer.getId()), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<InvoiceSummaryResponse> result = 
                invoiceQueryService.listForCustomer(customer.getId(), null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(invoiceRepository).findByCustomer_Id(eq(customer.getId()), any(Pageable.class));
    }

    @Test
    void listForCustomer_filtersbyStatus() {
        Page<Invoice> page = new PageImpl<>(List.of(invoice));
        when(invoiceRepository.findByCustomer_IdAndStatus(eq(customer.getId()), eq(Invoice.Status.PAID), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<InvoiceSummaryResponse> result = 
                invoiceQueryService.listForCustomer(customer.getId(), "PAID", 0, 10);

        assertNotNull(result);
        verify(invoiceRepository).findByCustomer_IdAndStatus(eq(customer.getId()), eq(Invoice.Status.PAID), any(Pageable.class));
    }

    @Test
    void getDetailForCustomer_returnsInvoiceDetail() {
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        InvoiceDetailResponse result = invoiceQueryService.getDetailForCustomer(invoice.getId(), customer.getId());

        assertNotNull(result);
        assertEquals(invoice.getId(), result.getInvoice().getId());
    }

    @Test
    void getDetailForCustomer_wrongCustomer_throws() {
        User otherCustomer = TestFixtures.user(User.Role.CUSTOMER);
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThrows(ForbiddenException.class,
                () -> invoiceQueryService.getDetailForCustomer(invoice.getId(), otherCustomer.getId()));
    }

    @Test
    void getDetailForCustomer_notFound_throws() {
        when(invoiceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> invoiceQueryService.getDetailForCustomer(UUID.randomUUID(), customer.getId()));
    }

    @Test
    void buildBreakdownForBooking_calculatesCorrectly() {
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(invoiceRepository.findByBooking_IdOrderByTypeAsc(booking.getId())).thenReturn(List.of(invoice));

        InvoiceBreakdownResponse result = invoiceQueryService.buildBreakdownForBooking(booking.getId(), customer.getId());

        assertNotNull(result);
        assertEquals(booking.getTotalAmount(), result.getTotalAmount());
        assertEquals(invoice.getAmount(), result.getPaidAmount());
        assertEquals(1, result.getInvoices().size());
    }

    @Test
    void buildBreakdownForBooking_wrongCustomer_throws() {
        User otherCustomer = TestFixtures.user(User.Role.CUSTOMER);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThrows(ForbiddenException.class,
                () -> invoiceQueryService.buildBreakdownForBooking(booking.getId(), otherCustomer.getId()));
    }

    @Test
    void buildBreakdownForManagerBooking_validatesScope() {
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(invoiceRepository.findByBooking_IdOrderByTypeAsc(booking.getId())).thenReturn(List.of(invoice));

        invoiceQueryService.buildBreakdownForManagerBooking(booking.getId(), manager);

        verify(scopeValidator).validateManagerAccess(manager, property.getId());
    }

    @Test
    void listForManager_requiresPropertyId() {
        assertThrows(IllegalArgumentException.class,
                () -> invoiceQueryService.listForManager(manager, null, null, 0, 10));
    }

    @Test
    void listForManager_validatesScope() {
        Page<Invoice> page = new PageImpl<>(List.of(invoice));
        when(invoiceRepository.findByPropertyIdAndOptionalStatus(eq(property.getId()), any(), any(Pageable.class)))
                .thenReturn(page);

        invoiceQueryService.listForManager(manager, property.getId(), null, 0, 10);

        verify(scopeValidator).validateManagerAccess(manager, property.getId());
    }

    @Test
    void buildBreakdown_calculatesRemainingBalance() {
        Invoice remainingInvoice = new Invoice();
        remainingInvoice.setId(UUID.randomUUID());
        remainingInvoice.setBooking(booking);
        remainingInvoice.setType(Invoice.Type.REMAINING_BALANCE);
        remainingInvoice.setStatus(Invoice.Status.UNPAID);
        remainingInvoice.setAmount(booking.getRemainingAmount());

        InvoiceBreakdownResponse result = invoiceQueryService.buildBreakdown(booking);
        
        // Initially empty, so remainingBalance should be zero
        assertEquals(BigDecimal.ZERO, result.getRemainingBalance());
    }
}
