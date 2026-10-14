package com.homestay.service;

import com.homestay.entity.Booking;
import com.homestay.entity.Payment;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingNoShowServiceTest {

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
    void markNoShow_confirmedPast24h_becomesNoShow_depositUnchanged() {
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.CONFIRMED);
        booking.setCheckInDate(LocalDate.now().minusDays(2));
        booking.setCheckOutDate(LocalDate.now().plusDays(1));

        Payment deposit = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID);

        when(bookingRepository.findConfirmedEligibleForNoShow(LocalDate.now()))
                .thenReturn(List.of(booking));
        when(roomRepository.hasBlockingBookings(room.getId())).thenReturn(false);

        int marked = service.markNoShowExpiredConfirmed();

        assertEquals(1, marked);
        assertEquals(Booking.Status.NO_SHOW, booking.getStatus());
        verify(bookingRepository).save(booking);
        assertEquals(Payment.Status.PAID, deposit.getStatus());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void markNoShow_notYetPast24h_skipped() {
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.CONFIRMED);
        booking.setCheckInDate(LocalDate.now());
        booking.setCheckOutDate(LocalDate.now().plusDays(2));

        when(bookingRepository.findConfirmedEligibleForNoShow(LocalDate.now()))
                .thenReturn(List.of(booking));

        int marked = service.markNoShowExpiredConfirmed();

        assertEquals(0, marked);
        assertEquals(Booking.Status.CONFIRMED, booking.getStatus());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void markNoShow_checkedIn_notInCandidateQuery_skipped() {
        // Repository only returns CONFIRMED; simulate empty candidates for CHECKED_IN
        when(bookingRepository.findConfirmedEligibleForNoShow(LocalDate.now()))
                .thenReturn(List.of());

        int marked = service.markNoShowExpiredConfirmed();

        assertEquals(0, marked);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void markNoShow_idempotent_alreadyConfirmedOnly() {
        Booking past = TestFixtures.booking(customer, room, Booking.Status.CONFIRMED);
        past.setCheckInDate(LocalDate.now().minusDays(3));

        when(bookingRepository.findConfirmedEligibleForNoShow(any(LocalDate.class)))
                .thenReturn(List.of(past));
        when(roomRepository.hasBlockingBookings(room.getId())).thenReturn(false);

        assertEquals(1, service.markNoShowExpiredConfirmed());

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertEquals(Booking.Status.NO_SHOW, captor.getValue().getStatus());
    }
}
