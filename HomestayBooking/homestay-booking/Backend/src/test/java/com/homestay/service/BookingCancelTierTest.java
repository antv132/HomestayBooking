package com.homestay.service;

import com.homestay.entity.Booking;
import com.homestay.entity.Payment;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.ForbiddenException;
import com.homestay.repository.BookingCheckVerificationRepository;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.ContractRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.PaymentRepository;
import com.homestay.repository.ReviewRepository;
import com.homestay.repository.RoomInspectionRepository;
import com.homestay.repository.RoomRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCancelTierTest {

    @Mock BookingRepository bookingRepository;
    @Mock NotificationService notificationService;
    @Mock RoomRepository roomRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock ReportPropertyScopeValidator scopeValidator;
    @Mock ManagerPropertyAssignmentRepository assignmentRepository;
    @Mock RoomInspectionRepository roomInspectionRepository;
    @Mock BookingCheckVerificationRepository checkVerificationRepository;
    @Mock HousekeepingTaskService housekeepingTaskService;
    @Mock ContractRepository contractRepository;
    @Mock com.homestay.repository.DamageReportRepository damageReportRepository;
    @Mock com.homestay.repository.RoomStatusBlockRepository roomStatusBlockRepository;
    @Mock InvoiceIssuanceService invoiceIssuanceService;
    @Mock InvoiceCancellationService invoiceCancellationService;
    @Mock InvoiceAdjustmentService invoiceAdjustmentService;
    @Mock InvoiceQueryService invoiceQueryService;
    @Mock InvoiceStatusSyncService invoiceStatusSyncService;
    @Mock com.homestay.repository.InvoiceRepository invoiceRepository;
    @Mock AdminSettingsService adminSettingsService;

    private BookingService service;
    private User customer;
    private Room room;

    @BeforeEach
    void setUp() {
        service = new BookingService(
                bookingRepository,
                notificationService,
                roomRepository,
                paymentRepository,
                reviewRepository,
                scopeValidator,
                assignmentRepository,
                roomInspectionRepository,
                checkVerificationRepository,
                housekeepingTaskService,
                contractRepository,
                damageReportRepository,
                roomStatusBlockRepository,
                invoiceIssuanceService,
                invoiceCancellationService,
                invoiceAdjustmentService,
                invoiceQueryService,
                invoiceStatusSyncService,
                invoiceRepository,
                adminSettingsService,
                10L,
                24L);
        lenient().when(adminSettingsService.getDepositPercentage()).thenReturn(new java.math.BigDecimal("0.40"));
        customer = TestFixtures.user(User.Role.CUSTOMER);
        Property property = TestFixtures.property();
        room = TestFixtures.room(property);
    }

    @Test
    void resolveTier_ge7_days_100() {
        Booking b = confirmedBooking(LocalDate.now().plusDays(7));
        assertEquals(100, BookingService.resolveCustomerCancelRefundPercent(b));
        b.setCheckInDate(LocalDate.now().plusDays(10));
        assertEquals(100, BookingService.resolveCustomerCancelRefundPercent(b));
    }

    @Test
    void resolveTier_3to6_days_50() {
        Booking b = confirmedBooking(LocalDate.now().plusDays(3));
        assertEquals(50, BookingService.resolveCustomerCancelRefundPercent(b));
        b.setCheckInDate(LocalDate.now().plusDays(6));
        assertEquals(50, BookingService.resolveCustomerCancelRefundPercent(b));
    }

    @Test
    void resolveTier_lt3_days_0() {
        Booking b = confirmedBooking(LocalDate.now().plusDays(2));
        assertEquals(0, BookingService.resolveCustomerCancelRefundPercent(b));
        b.setCheckInDate(LocalDate.now());
        assertEquals(0, BookingService.resolveCustomerCancelRefundPercent(b));
    }

    @Test
    void resolveTier_pendingDeposit_0() {
        Booking b = TestFixtures.booking(customer, room, Booking.Status.PENDING_DEPOSIT);
        b.setCheckInDate(LocalDate.now().plusDays(10));
        assertEquals(0, BookingService.resolveCustomerCancelRefundPercent(b));
    }

    @Test
    void preview_ge7_uses100PercentOfDeposit() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(8));
        Payment deposit = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID);
        deposit.setAmount(new BigDecimal("4000000"));

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of(deposit));

        var preview = service.getCancellationPreview(booking.getId(), customer);

        assertEquals(100, preview.getRefundPercent());
        assertEquals(0, new BigDecimal("4000000").compareTo(preview.getRefundAmount()));
        assertTrue(preview.getPolicyText().contains("100%"));
    }

    @Test
    void preview_3to6_uses50PercentOfDeposit() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(5));
        Payment deposit = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID);
        deposit.setAmount(new BigDecimal("4000000"));

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of(deposit));

        var preview = service.getCancellationPreview(booking.getId(), customer);

        assertEquals(50, preview.getRefundPercent());
        assertEquals(0, new BigDecimal("2000000").compareTo(preview.getRefundAmount()));
        assertEquals(0, new BigDecimal("2000000").compareTo(preview.getForfeitAmount()));
        assertTrue(preview.getPolicyText().contains("50%"));
    }

    @Test
    void cancel_3to6_days_marksDepositPartiallyRefundedAt50Percent() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(4));
        Payment deposit = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID);
        deposit.setAmount(new BigDecimal("4000000"));
        Payment remaining = TestFixtures.payment(booking, Payment.Type.REMAINING_BALANCE, Payment.Status.PAID);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of(deposit, remaining));
        when(contractRepository.findByBookingId(booking.getId())).thenReturn(Optional.empty());
        when(roomRepository.hasBlockingBookings(room.getId())).thenReturn(false);

        service.cancelBooking(booking.getId(), customer, "mid-window cancel");

        assertEquals(Booking.Status.CANCELLED, booking.getStatus());
        assertEquals(Payment.Status.PARTIALLY_REFUNDED, deposit.getStatus());
        assertEquals(0, new BigDecimal("4000000").compareTo(deposit.getAmount()));
        assertTrue(deposit.getVerificationNote() != null
                && deposit.getVerificationNote().contains("refundPercent=50")
                && deposit.getVerificationNote().contains("refundAmount=2000000")
                && deposit.getVerificationNote().contains("forfeitAmount=2000000"));
        assertEquals(Payment.Status.PAID, remaining.getStatus());
        assertTrue(remaining.getVerificationNote() != null
                && remaining.getVerificationNote().toLowerCase().contains("not auto-refunded"));
        verify(paymentRepository).saveAll(any());
    }

    @Test
    void cancel_ge7_days_marksDepositFullyRefunded() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(9));
        Payment deposit = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID);
        deposit.setAmount(new BigDecimal("4000000"));

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of(deposit));
        when(contractRepository.findByBookingId(booking.getId())).thenReturn(Optional.empty());
        when(roomRepository.hasBlockingBookings(room.getId())).thenReturn(false);

        service.cancelBooking(booking.getId(), customer, "early cancel");

        assertEquals(Booking.Status.CANCELLED, booking.getStatus());
        assertEquals(Payment.Status.REFUNDED, deposit.getStatus());
        assertEquals(0, new BigDecimal("4000000").compareTo(deposit.getAmount()));
        assertTrue(deposit.getVerificationNote() != null
                && deposit.getVerificationNote().contains("refundPercent=100")
                && deposit.getVerificationNote().contains("refundAmount=4000000")
                && deposit.getVerificationNote().contains("forfeitAmount=0"));
    }

    @Test
    void cancel_checkedIn_rejected() {
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.CHECKED_IN);
        booking.setCheckInDate(LocalDate.now());
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.cancelBooking(booking.getId(), customer, "test"));
        assertTrue(ex.getMessage().toLowerCase().contains("không thể hủy")
                || ex.getMessage().toLowerCase().contains("check-in"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancel_pendingDeposit_noRefundMark() {
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_DEPOSIT);
        booking.setCheckInDate(LocalDate.now().plusDays(5));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of());
        when(contractRepository.findByBookingId(booking.getId())).thenReturn(Optional.empty());
        when(roomRepository.hasBlockingBookings(room.getId())).thenReturn(false);

        service.cancelBooking(booking.getId(), customer, "changed plans");

        assertEquals(Booking.Status.CANCELLED, booking.getStatus());
        verify(paymentRepository, never()).saveAll(any());
    }

    @Test
    void cancel_lt3_days_depositNotMarkedRefunded() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(1));
        Payment deposit = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID);
        deposit.setAmount(new BigDecimal("4000000"));

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of(deposit));
        when(contractRepository.findByBookingId(booking.getId())).thenReturn(Optional.empty());
        when(roomRepository.hasBlockingBookings(room.getId())).thenReturn(false);

        service.cancelBooking(booking.getId(), customer, "late cancel");

        assertEquals(Booking.Status.CANCELLED, booking.getStatus());
        assertEquals(Payment.Status.PAID, deposit.getStatus());
        assertTrue(deposit.getVerificationNote() != null
                && deposit.getVerificationNote().toLowerCase().contains("forfeit")
                && deposit.getVerificationNote().contains("refundPercent=0")
                && deposit.getVerificationNote().contains("refundAmount=0.00"));
    }

    @Test
    void nonCustomer_preview_forbidden() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(10));
        User manager = TestFixtures.user(User.Role.MANAGER);

        assertThrows(ForbiddenException.class,
                () -> service.getCancellationPreview(booking.getId(), manager));
    }

    private Booking confirmedBooking(LocalDate checkIn) {
        Booking b = TestFixtures.booking(customer, room, Booking.Status.CONFIRMED);
        b.setCheckInDate(checkIn);
        b.setCheckOutDate(checkIn.plusDays(2));
        return b;
    }
}
