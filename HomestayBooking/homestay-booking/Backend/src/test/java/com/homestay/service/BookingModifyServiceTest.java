package com.homestay.service;

import com.homestay.dto.request.ModifyBookingRequest;
import com.homestay.entity.Booking;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.ConflictException;
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
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingModifyServiceTest {

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
    private User manager;
    private User customer;
    private Property property;
    private Room room;
    private Booking booking;

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
                10,
                24);
        lenient().when(adminSettingsService.getDepositPercentage()).thenReturn(new java.math.BigDecimal("0.40"));
        manager = TestFixtures.user(User.Role.MANAGER);
        customer = TestFixtures.user(User.Role.CUSTOMER);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
        room.setPricePerNight(new BigDecimal("1000000"));
        room.setCapacity(4);
        room.setStatus(Room.Status.AVAILABLE);
        booking = TestFixtures.booking(customer, room, Booking.Status.CONFIRMED);
        booking.setCheckInDate(LocalDate.now().plusDays(10));
        booking.setCheckOutDate(LocalDate.now().plusDays(12));
        booking.setTotalAmount(new BigDecimal("2000000"));
        booking.setDepositAmount(new BigDecimal("800000"));
        booking.setRemainingAmount(new BigDecimal("1200000"));
        booking.setGuestCount(2);
        org.mockito.Mockito.lenient()
                .when(roomStatusBlockRepository.existsActiveOverlappingStay(any(), any(), any()))
                .thenReturn(false);
        org.mockito.Mockito.lenient().when(invoiceIssuanceService.issuePair(any())).thenReturn(java.util.Collections.emptyList());
        org.mockito.Mockito.lenient().when(invoiceQueryService.buildBreakdown(any())).thenReturn(new com.homestay.dto.response.InvoiceBreakdownResponse());
        org.mockito.Mockito.lenient().when(invoiceCancellationService.cancelUnpaidForBooking(any())).thenReturn(0);
    }

    @Test
    void modify_nonConfirmed_rejected() {
        booking.setStatus(Booking.Status.CHECKED_IN);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());

        ModifyBookingRequest req = new ModifyBookingRequest();
        req.setCheckIn(LocalDate.now().plusDays(11));
        req.setCheckOut(LocalDate.now().plusDays(13));

        assertThrows(ConflictException.class,
                () -> service.modifyBookingForManager(manager, booking.getId(), req));
    }

    @Test
    void modify_statusBlockOverlap_conflict() {
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        when(roomStatusBlockRepository.existsActiveOverlappingStay(eq(room.getId()), any(), any()))
                .thenReturn(true);

        ModifyBookingRequest req = new ModifyBookingRequest();
        req.setCheckIn(LocalDate.now().plusDays(11));
        req.setCheckOut(LocalDate.now().plusDays(13));

        assertThrows(ConflictException.class,
                () -> service.modifyBookingForManager(manager, booking.getId(), req));
        verify(roomRepository, never()).existsOverlapBookingExcluding(any(), any(), any(), any());
    }

    @Test
    void modify_dates_recalculatesPriceDelta() {
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.existsOverlapBookingExcluding(
                eq(room.getId()), any(), any(), eq(booking.getId()))).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(Collections.emptyList());
        when(reviewRepository.existsByBooking_Id(booking.getId())).thenReturn(false);
        when(roomInspectionRepository.findByBookingId(booking.getId()))
                .thenReturn(Optional.empty());

        ModifyBookingRequest req = new ModifyBookingRequest();
        req.setCheckIn(LocalDate.now().plusDays(10));
        req.setCheckOut(LocalDate.now().plusDays(13)); // 3 nights vs 2

        var resp = service.modifyBookingForManager(manager, booking.getId(), req);

        assertEquals(0, new BigDecimal("1000000").compareTo(resp.getPriceDelta()));
        assertEquals(0, new BigDecimal("3000000").compareTo(booking.getTotalAmount()));
        assertTrue(resp.getMessage().toLowerCase().contains("cập nhật")
                || resp.getMessage().toLowerCase().contains("thanh toán"));
        verify(notificationService).sendNotification(
                eq(customer.getId()), any(), any(), any(), eq(booking.getId()), eq("Booking"));
    }
}
