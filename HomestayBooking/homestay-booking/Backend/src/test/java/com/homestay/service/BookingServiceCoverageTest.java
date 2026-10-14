package com.homestay.service;

import com.homestay.dto.request.CreateBookingRequest;
import com.homestay.dto.request.ManagerCheckInRequest;
import com.homestay.dto.request.ManagerCheckOutRequest;
import com.homestay.dto.request.ModifyBookingRequest;
import com.homestay.dto.response.BookingDetailResponse;
import com.homestay.dto.response.CancellationPreviewResponse;
import com.homestay.dto.response.ManagerBookingDetailResponse;
import com.homestay.dto.response.ModifyBookingResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.Contract;
import com.homestay.entity.Payment;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.RoomInspection;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ConflictException;
import com.homestay.exception.ForbiddenException;
import com.homestay.exception.ResourceNotFoundException;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingServiceCoverageTest {

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
    private User manager;
    private Property property;
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
        when(adminSettingsService.getDepositPercentage()).thenReturn(new BigDecimal("0.40"));
        customer = TestFixtures.user(User.Role.CUSTOMER);
        manager = TestFixtures.user(User.Role.MANAGER);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
        room.setStatus(Room.Status.AVAILABLE);
        room.setPricePerNight(new BigDecimal("1000000"));
        room.setCapacity(4);
        room.setRoomType("Deluxe");
        org.mockito.Mockito.lenient()
                .when(roomStatusBlockRepository.existsActiveOverlappingStay(any(), any(), any()))
                .thenReturn(false);
        org.mockito.Mockito.lenient().when(invoiceIssuanceService.issuePair(any())).thenReturn(java.util.Collections.emptyList());
        org.mockito.Mockito.lenient().when(invoiceQueryService.buildBreakdown(any())).thenReturn(new com.homestay.dto.response.InvoiceBreakdownResponse());
        org.mockito.Mockito.lenient().when(invoiceCancellationService.cancelUnpaidForBooking(any())).thenReturn(0);
    }

    // ── createBooking ────────────────────────────────────────────────────────

    @Test
    void createBooking_success_setsHoldExpiresAtAndPendingDeposit() {
        CreateBookingRequest req = createRequest(LocalDate.now().plusDays(2), LocalDate.now().plusDays(4), 2);
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.existsOverlapBooking(eq(room.getId()), any(), any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            if (b.getId() == null) {
                b.setId(UUID.randomUUID());
            }
            return b;
        });

        LocalDateTime before = LocalDateTime.now();
        BookingDetailResponse resp = service.createBooking(req, customer);
        LocalDateTime after = LocalDateTime.now();

        assertEquals(Booking.Status.PENDING_DEPOSIT.name(), resp.getStatus());
        assertNotNull(resp.getHoldExpiresAt());
        assertTrue(!resp.getHoldExpiresAt().isBefore(before.plusMinutes(10).minusSeconds(2)));
        assertTrue(!resp.getHoldExpiresAt().isAfter(after.plusMinutes(10).plusSeconds(2)));
        assertEquals(0, new BigDecimal("2000000").compareTo(resp.getTotalAmount()));
        assertEquals(0, new BigDecimal("800000").compareTo(resp.getDepositAmount()));
        assertEquals(0, new BigDecimal("1200000").compareTo(resp.getRemainingAmount()));
    }

    @Test
    void createBooking_overlap_throws409() {
        CreateBookingRequest req = createRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), 2);
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.existsOverlapBooking(eq(room.getId()), any(), any())).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.createBooking(req, customer));
        assertTrue(ex.getMessage().toLowerCase().contains("giữ")
                || ex.getMessage().toLowerCase().contains("trùng")
                || ex.getMessage().toLowerCase().contains("khoảng"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_checkoutNotAfterCheckin_rejected() {
        CreateBookingRequest req = createRequest(LocalDate.now().plusDays(2), LocalDate.now().plusDays(2), 1);
        assertThrows(IllegalArgumentException.class, () -> service.createBooking(req, customer));
    }

    @Test
    void createBooking_checkInInPast_rejected() {
        CreateBookingRequest req = createRequest(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 1);
        assertThrows(IllegalArgumentException.class, () -> service.createBooking(req, customer));
    }

    @Test
    void createBooking_nonCustomer_forbidden() {
        CreateBookingRequest req = createRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), 1);
        assertThrows(ForbiddenException.class, () -> service.createBooking(req, manager));
    }

    @Test
    void createBooking_roomNotFound_404() {
        CreateBookingRequest req = createRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), 1);
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.createBooking(req, customer));
    }

    @Test
    void createBooking_blockedRoomStatus_conflict() {
        room.setStatus(Room.Status.MAINTENANCE);
        CreateBookingRequest req = createRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), 1);
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));

        assertThrows(ConflictException.class, () -> service.createBooking(req, customer));
    }

    @Test
    void createBooking_statusBlockOverlap_conflict() {
        CreateBookingRequest req = createRequest(LocalDate.now().plusDays(2), LocalDate.now().plusDays(4), 2);
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        when(roomStatusBlockRepository.existsActiveOverlappingStay(eq(room.getId()), any(), any()))
                .thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.createBooking(req, customer));
        assertTrue(ex.getMessage().toLowerCase().contains("bảo trì")
                || ex.getMessage().toLowerCase().contains("ngưng"));
        verify(bookingRepository, never()).save(any());
    }

    /** Half-open: prior stay ending on D must not block new stay starting on D (repo returns false). */
    @Test
    void createBooking_sameDayTurnover_allowedWhenNoOverlap() {
        LocalDate turnover = LocalDate.now().plusDays(5);
        CreateBookingRequest req = createRequest(turnover, turnover.plusDays(2), 2);
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.existsOverlapBooking(eq(room.getId()), eq(turnover), eq(turnover.plusDays(2))))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            if (b.getId() == null) {
                b.setId(UUID.randomUUID());
            }
            return b;
        });

        BookingDetailResponse resp = service.createBooking(req, customer);
        assertEquals(Booking.Status.PENDING_DEPOSIT.name(), resp.getStatus());
        verify(roomRepository).existsOverlapBooking(room.getId(), turnover, turnover.plusDays(2));
        verify(roomStatusBlockRepository).existsActiveOverlappingStay(room.getId(), turnover, turnover.plusDays(2));
    }

    @Test
    void createBooking_guestCountExceedsCapacity_rejected() {
        CreateBookingRequest req = createRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), 99);
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));

        assertThrows(IllegalArgumentException.class, () -> service.createBooking(req, customer));
    }

    // ── manager cancel 100% ──────────────────────────────────────────────────

    @Test
    void cancelBookingForManager_confirmed_refunds100PercentDeposit() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(10));
        Payment deposit = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID);
        Contract contract = new Contract();
        contract.setId(UUID.randomUUID());
        contract.setStatus(Contract.Status.ACTIVE);
        contract.setBooking(booking);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of(deposit));
        when(contractRepository.findByBookingId(booking.getId())).thenReturn(Optional.of(contract));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewRepository.existsByBooking_Id(booking.getId())).thenReturn(false);
        when(roomInspectionRepository.findByBookingId(booking.getId())).thenReturn(Optional.empty());

        ManagerBookingDetailResponse resp =
                service.cancelBookingForManager(manager, booking.getId(), "System error / force majeure");

        assertEquals(Booking.Status.CANCELLED.name(), resp.getStatus());
        assertEquals(Payment.Status.REFUNDED, deposit.getStatus());
        assertEquals(Contract.Status.CANCELLED, contract.getStatus());
        assertTrue(deposit.getVerificationNote() != null && deposit.getVerificationNote().contains("100%"));
        verify(notificationService).sendNotification(
                eq(customer.getId()), any(), any(), any(), eq(booking.getId()), eq("Booking"));
    }

    @Test
    void cancelBookingForManager_blankReason_rejected() {
        assertThrows(BusinessException.class,
                () -> service.cancelBookingForManager(manager, UUID.randomUUID(), "  "));
    }

    @Test
    void getManagerCancellationPreview_returns100Percent() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(5));
        Payment deposit = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID);
        deposit.setAmount(new BigDecimal("4000000"));

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of(deposit));

        CancellationPreviewResponse preview =
                service.getManagerCancellationPreview(manager, booking.getId());

        assertEquals(100, preview.getRefundPercent());
        assertEquals(0, new BigDecimal("4000000").compareTo(preview.getRefundAmount()));
        assertTrue(preview.getPolicyText().toLowerCase().contains("100%"));
    }

    // ── customer cancel pending deposit ──────────────────────────────────────

    @Test
    void cancelBooking_pendingDeposit_noRefund_andVoidsPendingPayment() {
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_DEPOSIT);
        booking.setCheckInDate(LocalDate.now().plusDays(5));
        booking.setCheckOutDate(LocalDate.now().plusDays(7));
        Payment pendingDeposit = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PENDING);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of(pendingDeposit));
        when(contractRepository.findByBookingId(booking.getId())).thenReturn(Optional.empty());

        service.cancelBooking(booking.getId(), customer, "changed mind");

        assertEquals(Booking.Status.CANCELLED, booking.getStatus());
        assertEquals(Payment.Status.FAILED, pendingDeposit.getStatus());
        verify(paymentRepository).saveAll(any());
    }

    @Test
    void cancelBooking_managerViaCustomerApi_forbidden() {
        assertThrows(ForbiddenException.class,
                () -> service.cancelBooking(UUID.randomUUID(), manager, "nope"));
    }

    @Test
    void cancelBooking_confirmedGe7_marksDepositRefunded() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(10));
        Payment deposit = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID);
        Payment remaining = TestFixtures.payment(booking, Payment.Type.REMAINING_BALANCE, Payment.Status.PAID);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of(deposit, remaining));
        when(contractRepository.findByBookingId(booking.getId())).thenReturn(Optional.empty());

        service.cancelBooking(booking.getId(), customer, "plans changed");

        assertEquals(Booking.Status.CANCELLED, booking.getStatus());
        assertEquals(Payment.Status.REFUNDED, deposit.getStatus());
        assertEquals(Payment.Status.PAID, remaining.getStatus());
        assertTrue(remaining.getVerificationNote() != null
                && remaining.getVerificationNote().toLowerCase().contains("not auto-refunded"));
    }

    // ── check-in unpaid block + success with cash ────────────────────────────

    @Test
    void markAsCheckedIn_remainingUnpaidWithoutCollection_conflict() {
        Booking booking = confirmedBooking(LocalDate.now());
        booking.setRemainingAmount(new BigDecimal("6000000"));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of(TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID)));

        ManagerCheckInRequest req = checkInRequest(false);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.markAsCheckedInForManager(manager, booking.getId(), req));
        assertTrue(ex.getMessage().contains("Chưa thu phần còn lại"));
    }

    @Test
    void markAsCheckedIn_remainingCollectedCash_success() {
        Booking booking = confirmedBooking(LocalDate.now());
        booking.setRemainingAmount(new BigDecimal("6000000"));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of(TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID)));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checkVerificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reviewRepository.existsByBooking_Id(booking.getId())).thenReturn(false);
        when(roomInspectionRepository.findByBookingId(booking.getId())).thenReturn(Optional.empty());

        ManagerCheckInRequest req = checkInRequest(true);
        ManagerBookingDetailResponse resp =
                service.markAsCheckedInForManager(manager, booking.getId(), req);

        assertEquals(Booking.Status.CHECKED_IN.name(), resp.getStatus());
        assertEquals(Room.Status.OCCUPIED, room.getStatus());
        ArgumentCaptor<Payment> payCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(payCaptor.capture());
        assertEquals(Payment.Type.REMAINING_BALANCE, payCaptor.getValue().getType());
        assertEquals(Payment.Method.CASH, payCaptor.getValue().getMethod());
        assertEquals(Payment.Status.PAID, payCaptor.getValue().getStatus());
    }

    @Test
    void markAsCheckedIn_keyNotHanded_rejected() {
        Booking booking = confirmedBooking(LocalDate.now());
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());

        ManagerCheckInRequest req = checkInRequest(true);
        req.setKeyHandedOver(false);

        assertThrows(BusinessException.class,
                () -> service.markAsCheckedInForManager(manager, booking.getId(), req));
    }

    @Test
    void markAsCheckedIn_beforeCheckInDate_rejected() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(2));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());

        assertThrows(BusinessException.class,
                () -> service.markAsCheckedInForManager(manager, booking.getId(), checkInRequest(true)));
    }

    @Test
    void markAsCheckedIn_notConfirmed_rejected() {
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_DEPOSIT);
        booking.setCheckInDate(LocalDate.now());
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());

        assertThrows(BusinessException.class,
                () -> service.markAsCheckedInForManager(manager, booking.getId(), checkInRequest(true)));
    }

    // ── check-out without inspection ─────────────────────────────────────────

    @Test
    void markAsCheckedOut_pendingInspectionWithoutPassed_conflict() {
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_INSPECTION);
        RoomInspection inspection = TestFixtures.inspection(booking, property, room);
        inspection.setStatus(RoomInspection.Status.PENDING);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());
        when(roomInspectionRepository.findByBookingId(booking.getId())).thenReturn(Optional.of(inspection));

        ManagerCheckOutRequest req = new ManagerCheckOutRequest();
        req.setKeyReturned(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.markAsCheckedOutForManager(manager, booking.getId(), req));
        assertTrue(ex.getMessage().toLowerCase().contains("kiểm tra"));
    }

    @Test
    void markAsCheckedOut_pendingInspectionMissing_conflict() {
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_INSPECTION);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());
        when(roomInspectionRepository.findByBookingId(booking.getId())).thenReturn(Optional.empty());

        ManagerCheckOutRequest req = new ManagerCheckOutRequest();
        req.setKeyReturned(true);

        assertThrows(ConflictException.class,
                () -> service.markAsCheckedOutForManager(manager, booking.getId(), req));
    }

    @Test
    void markAsCheckedOut_checkedIn_movesToPendingInspection() {
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.CHECKED_IN);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());
        when(roomInspectionRepository.findByBookingId(booking.getId())).thenReturn(Optional.empty());
        when(roomInspectionRepository.save(any(RoomInspection.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checkVerificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reviewRepository.existsByBooking_Id(booking.getId())).thenReturn(false);
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(Collections.emptyList());

        ManagerCheckOutRequest req = new ManagerCheckOutRequest();
        req.setKeyReturned(true);

        ManagerBookingDetailResponse resp =
                service.markAsCheckedOutForManager(manager, booking.getId(), req);

        assertEquals(Booking.Status.PENDING_INSPECTION.name(), resp.getStatus());
        verify(roomInspectionRepository).save(any(RoomInspection.class));
    }

    @Test
    void markAsCheckedOut_inspectionPassed_completesCheckout() {
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_INSPECTION);
        RoomInspection inspection = TestFixtures.inspection(booking, property, room);
        inspection.setStatus(RoomInspection.Status.PASSED);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());
        when(roomInspectionRepository.findByBookingId(booking.getId())).thenReturn(Optional.of(inspection));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checkVerificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reviewRepository.existsByBooking_Id(booking.getId())).thenReturn(false);
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(Collections.emptyList());

        ManagerCheckOutRequest req = new ManagerCheckOutRequest();
        req.setKeyReturned(true);

        ManagerBookingDetailResponse resp =
                service.markAsCheckedOutForManager(manager, booking.getId(), req);

        assertEquals(Booking.Status.CHECKED_OUT.name(), resp.getStatus());
        assertEquals(Room.Status.PENDING_CLEANING, room.getStatus());
        verify(housekeepingTaskService).onBookingCheckedOut(booking);
        verify(contractRepository).findByBookingId(booking.getId());
    }

    @Test
    void markAsCheckedOut_inspectionPassed_completesActiveContract() {
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_INSPECTION);
        RoomInspection inspection = TestFixtures.inspection(booking, property, room);
        inspection.setStatus(RoomInspection.Status.PASSED);
        Contract active = new Contract();
        active.setId(UUID.randomUUID());
        active.setBooking(booking);
        active.setStatus(Contract.Status.ACTIVE);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());
        when(roomInspectionRepository.findByBookingId(booking.getId())).thenReturn(Optional.of(inspection));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checkVerificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reviewRepository.existsByBooking_Id(booking.getId())).thenReturn(false);
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(Collections.emptyList());
        when(contractRepository.findByBookingId(booking.getId())).thenReturn(Optional.of(active));
        when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

        ManagerCheckOutRequest req = new ManagerCheckOutRequest();
        req.setKeyReturned(true);

        ManagerBookingDetailResponse resp =
                service.markAsCheckedOutForManager(manager, booking.getId(), req);

        assertEquals(Booking.Status.CHECKED_OUT.name(), resp.getStatus());
        assertEquals(Contract.Status.COMPLETED, active.getStatus());
        verify(contractRepository).save(active);
    }

    @Test
    void markAsCheckedOut_failedWithDamage_unpaid_conflict() {
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_INSPECTION);
        RoomInspection inspection = TestFixtures.inspection(booking, property, room);
        inspection.setStatus(RoomInspection.Status.FAILED_WITH_DAMAGE);
        com.homestay.entity.DamageReport report =
                TestFixtures.damageReport(booking, inspection, new BigDecimal("500000"), false);
        report.setStatus(com.homestay.entity.DamageReport.Status.APPROVED);
        Payment unpaid = TestFixtures.payment(booking, Payment.Type.DAMAGE_FEE, Payment.Status.PENDING);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());
        when(roomInspectionRepository.findByBookingId(booking.getId())).thenReturn(Optional.of(inspection));
        when(damageReportRepository.findFirstByBooking_IdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(Optional.of(report));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of(unpaid));

        ManagerCheckOutRequest req = new ManagerCheckOutRequest();
        req.setKeyReturned(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.markAsCheckedOutForManager(manager, booking.getId(), req));
        assertTrue(ex.getMessage().toLowerCase().contains("thiệt hại")
                || ex.getMessage().toLowerCase().contains("thanh toán"));
        verify(housekeepingTaskService, never()).onBookingCheckedOut(any());
    }

    @Test
    void markAsCheckedOut_failedWithDamage_paid_completesCheckout() {
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_INSPECTION);
        booking.setDamageFeeAmount(new BigDecimal("500000"));
        RoomInspection inspection = TestFixtures.inspection(booking, property, room);
        inspection.setStatus(RoomInspection.Status.FAILED_WITH_DAMAGE);
        com.homestay.entity.DamageReport report =
                TestFixtures.damageReport(booking, inspection, new BigDecimal("500000"), false);
        report.setStatus(com.homestay.entity.DamageReport.Status.PAID);
        Payment paid = TestFixtures.payment(booking, Payment.Type.DAMAGE_FEE, Payment.Status.PAID);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());
        when(roomInspectionRepository.findByBookingId(booking.getId())).thenReturn(Optional.of(inspection));
        when(damageReportRepository.findFirstByBooking_IdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(Optional.of(report));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of(paid));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checkVerificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reviewRepository.existsByBooking_Id(booking.getId())).thenReturn(false);

        ManagerCheckOutRequest req = new ManagerCheckOutRequest();
        req.setKeyReturned(true);

        ManagerBookingDetailResponse resp =
                service.markAsCheckedOutForManager(manager, booking.getId(), req);

        assertEquals(Booking.Status.CHECKED_OUT.name(), resp.getStatus());
        assertEquals(Room.Status.PENDING_CLEANING, room.getStatus());
        verify(housekeepingTaskService).onBookingCheckedOut(booking);
    }

    @Test
    void markAsCheckedOut_pendingDamagePayment_conflict() {
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_DAMAGE_PAYMENT);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());

        ManagerCheckOutRequest req = new ManagerCheckOutRequest();
        req.setKeyReturned(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.markAsCheckedOutForManager(manager, booking.getId(), req));
        assertTrue(ex.getMessage().toLowerCase().contains("thiệt hại"));
    }

    @Test
    void markAsCheckedOut_keyNotReturned_rejected() {
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.CHECKED_IN);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());

        ManagerCheckOutRequest req = new ManagerCheckOutRequest();
        req.setKeyReturned(false);

        assertThrows(BusinessException.class,
                () -> service.markAsCheckedOutForManager(manager, booking.getId(), req));
    }

    // ── modify confirmed success / conflict ──────────────────────────────────

    @Test
    void modifyBooking_overlap_conflict() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(10));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.existsOverlapBookingExcluding(
                eq(room.getId()), any(), any(), eq(booking.getId()))).thenReturn(true);

        ModifyBookingRequest req = new ModifyBookingRequest();
        req.setCheckIn(LocalDate.now().plusDays(11));
        req.setCheckOut(LocalDate.now().plusDays(13));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.modifyBookingForManager(manager, booking.getId(), req));
        assertTrue(ex.getMessage().toLowerCase().contains("trùng"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void modifyBooking_confirmed_sameTotal_zeroDeltaMessage() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(10));
        booking.setTotalAmount(new BigDecimal("2000000"));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.existsOverlapBookingExcluding(
                eq(room.getId()), any(), any(), eq(booking.getId()))).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(Collections.emptyList());
        when(reviewRepository.existsByBooking_Id(booking.getId())).thenReturn(false);
        when(roomInspectionRepository.findByBookingId(booking.getId())).thenReturn(Optional.empty());

        ModifyBookingRequest req = new ModifyBookingRequest();
        req.setCheckIn(LocalDate.now().plusDays(15));
        req.setCheckOut(LocalDate.now().plusDays(17)); // still 2 nights @ 1M

        ModifyBookingResponse resp = service.modifyBookingForManager(manager, booking.getId(), req);

        assertEquals(0, BigDecimal.ZERO.compareTo(resp.getPriceDelta()));
        assertTrue(resp.getMessage().toLowerCase().contains("không đổi"));
    }

    @Test
    void modifyBooking_blockedRoom_conflict() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(10));
        Room other = TestFixtures.room(property);
        other.setStatus(Room.Status.OUT_OF_SERVICE);
        other.setPricePerNight(new BigDecimal("1000000"));
        other.setCapacity(4);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());
        when(roomRepository.findByIdForUpdate(other.getId())).thenReturn(Optional.of(other));

        ModifyBookingRequest req = new ModifyBookingRequest();
        req.setRoomId(other.getId());

        assertThrows(ConflictException.class,
                () -> service.modifyBookingForManager(manager, booking.getId(), req));
    }

    @Test
    void modifyBooking_invalidDateRange_rejected() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(10));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());

        ModifyBookingRequest req = new ModifyBookingRequest();
        req.setCheckIn(LocalDate.now().plusDays(12));
        req.setCheckOut(LocalDate.now().plusDays(12));

        assertThrows(IllegalArgumentException.class,
                () -> service.modifyBookingForManager(manager, booking.getId(), req));
    }

    // ── hold timeout job ─────────────────────────────────────────────────────

    @Test
    void cancelExpiredDepositHolds_unpaid_cancels() {
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_DEPOSIT);
        booking.setHoldExpiresAt(LocalDateTime.now().minusMinutes(1));
        room.setStatus(Room.Status.PENDING_DEPOSIT);

        when(bookingRepository.findExpiredPendingDeposits(any(LocalDateTime.class)))
                .thenReturn(List.of(booking));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of());
        when(roomRepository.hasBlockingBookings(room.getId())).thenReturn(false);

        int cancelled = service.cancelExpiredDepositHolds();

        assertEquals(1, cancelled);
        assertEquals(Booking.Status.CANCELLED, booking.getStatus());
        assertEquals(Room.Status.AVAILABLE, room.getStatus());
        verify(notificationService).sendNotification(
                eq(customer.getId()), any(), any(), any(), eq(booking.getId()), eq("Booking"));
    }

    @Test
    void cancelExpiredDepositHolds_depositAlreadyPaid_skipped() {
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_DEPOSIT);
        when(bookingRepository.findExpiredPendingDeposits(any(LocalDateTime.class)))
                .thenReturn(List.of(booking));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of(TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID)));

        int cancelled = service.cancelExpiredDepositHolds();

        assertEquals(0, cancelled);
        assertEquals(Booking.Status.PENDING_DEPOSIT, booking.getStatus());
        verify(bookingRepository, never()).save(any());
    }

    // ── list / detail / pageable ─────────────────────────────────────────────

    @Test
    void getMyBookings_customer_withStatusFilter() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(3));
        when(bookingRepository.findByCustomerIdAndStatus(eq(customer.getId()), eq(Booking.Status.CONFIRMED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));
        when(reviewRepository.existsByBooking_Id(booking.getId())).thenReturn(false);

        PageResponse<?> page = service.getMyBookings(customer, 0, 10, "CONFIRMED", "createdAt,desc");

        assertEquals(1, page.getContent().size());
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void getMyBookings_nonCustomer_forbidden() {
        assertThrows(ForbiddenException.class,
                () -> service.getMyBookings(manager, 0, 10, null, null));
    }

    @Test
    void getAllBookings_filtersByStatus() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(1));
        when(bookingRepository.findAllWithFilters(eq(Booking.Status.CONFIRMED), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));
        when(reviewRepository.existsByBooking_Id(booking.getId())).thenReturn(true);

        PageResponse<?> page = service.getAllBookings(0, 10, "CONFIRMED", null, "checkInDate,asc");

        assertEquals(1, page.getContent().size());
    }

    @Test
    void getBookingsForManagerScoped_emptyAssignments_returnsEmpty() {
        when(assignmentRepository.findActivePropertiesByManagerId(manager.getId()))
                .thenReturn(List.of());

        PageResponse<?> page = service.getBookingsForManagerScoped(
                manager, null, "ALL", null, null, null, 0, 10, null);

        assertEquals(0, page.getTotalElements());
        assertTrue(page.getContent().isEmpty());
    }

    @Test
    void getBookingsForManagerScoped_withPropertyId() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(2));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), eq(property.getId()));
        when(bookingRepository.findForManagerWithFilters(
                eq(List.of(property.getId())), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));
        when(reviewRepository.existsByBooking_Id(booking.getId())).thenReturn(false);

        PageResponse<?> page = service.getBookingsForManagerScoped(
                manager, property.getId().toString(), null, null, null, null, 0, 10, "checkInDate,desc");

        assertEquals(1, page.getContent().size());
    }

    @Test
    void getBookingDetail_owner_success() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(1));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBooking_Id(booking.getId())).thenReturn(false);
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(Collections.emptyList());

        BookingDetailResponse resp = service.getBookingDetail(booking.getId(), customer);

        assertEquals(booking.getId(), resp.getId());
        assertNotNull(resp.getPayments());
    }

    @Test
    void getBookingDetail_otherCustomer_forbidden() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(1));
        User other = TestFixtures.user(User.Role.CUSTOMER);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThrows(ForbiddenException.class,
                () -> service.getBookingDetail(booking.getId(), other));
    }

    @Test
    void getBookingDetailForManager_scoped() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(1));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), any());
        when(reviewRepository.existsByBooking_Id(booking.getId())).thenReturn(false);
        when(roomInspectionRepository.findByBookingId(booking.getId())).thenReturn(Optional.empty());
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(Collections.emptyList());

        ManagerBookingDetailResponse resp =
                service.getBookingDetailForManager(manager, booking.getId());

        assertEquals(booking.getId(), resp.getId());
    }

    @Test
    void buildPageable_blankDefaultsToCreatedAtDesc() {
        Pageable p = BookingService.buildPageable(0, 20, null);
        assertEquals("createdAt", p.getSort().iterator().next().getProperty());
        assertTrue(p.getSort().getOrderFor("createdAt").isDescending());
    }

    @Test
    void buildPageable_disallowedFieldFallsBack() {
        Pageable p = BookingService.buildPageable(1, 5, "hackField,asc");
        assertEquals("createdAt", p.getSort().iterator().next().getProperty());
    }

    @Test
    void buildPageable_allowedAsc() {
        Pageable p = BookingService.buildPageable(0, 10, "totalAmount,asc");
        assertEquals("totalAmount", p.getSort().iterator().next().getProperty());
        assertTrue(p.getSort().getOrderFor("totalAmount").isAscending());
    }

    // ── legacy + cancel guards ───────────────────────────────────────────────

    @Test
    void markAsCheckedIn_legacy_blocked() {
        assertThrows(BusinessException.class, () -> service.markAsCheckedIn(UUID.randomUUID()));
    }

    @Test
    void markAsCheckedOut_legacy_blocked() {
        assertThrows(BusinessException.class, () -> service.markAsCheckedOut(UUID.randomUUID()));
    }

    @Test
    void cancelBooking_alreadyCancelled_rejected() {
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.CANCELLED);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThrows(IllegalArgumentException.class,
                () -> service.cancelBooking(booking.getId(), customer));
    }

    @Test
    void cancelBooking_wrongOwner_forbidden() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(5));
        User other = TestFixtures.user(User.Role.CUSTOMER);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThrows(ForbiddenException.class,
                () -> service.cancelBooking(booking.getId(), other));
    }

    @Test
    void getCancellationPreview_pendingDeposit_zeroRefund() {
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_DEPOSIT);
        booking.setCheckInDate(LocalDate.now().plusDays(10));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of());

        CancellationPreviewResponse preview =
                service.getCancellationPreview(booking.getId(), customer);

        assertEquals(0, preview.getRefundPercent());
        assertEquals(0, BigDecimal.ZERO.compareTo(preview.getRefundAmount()));
    }

    @Test
    void getCancellationPreview_midTier_50() {
        Booking booking = confirmedBooking(LocalDate.now().plusDays(4));
        Payment deposit = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID);
        deposit.setAmount(new BigDecimal("4000000"));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of(deposit));

        CancellationPreviewResponse preview =
                service.getCancellationPreview(booking.getId(), customer);

        assertEquals(50, preview.getRefundPercent());
        assertEquals(0, new BigDecimal("2000000.00").compareTo(preview.getRefundAmount()));
    }

    // ── ID document upload (SCR-37) ──────────────────────────────────────────

    @Test
    void uploadIdDocuments_success_savesJpgAndReturnsUrls() throws Exception {
        Booking booking = confirmedBooking(LocalDate.now());
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), eq(property.getId()));

        MockMultipartFile file = new MockMultipartFile(
                "files", "cmnd.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});

        List<String> urls = service.uploadIdDocumentsForManager(manager, booking.getId(), List.of(file));

        assertEquals(1, urls.size());
        assertTrue(urls.get(0).startsWith("/uploads/bookings/" + booking.getId() + "/"));
        assertTrue(urls.get(0).endsWith("_cmnd.jpg"));
        Path saved = Path.of("uploads", "bookings", booking.getId().toString()).toAbsolutePath();
        assertTrue(Files.isDirectory(saved));
        // cleanup test files
        try (var stream = Files.list(saved)) {
            stream.forEach(p -> {
                try { Files.deleteIfExists(p); } catch (Exception ignored) { }
            });
        }
        Files.deleteIfExists(saved);
    }

    @Test
    void uploadIdDocuments_blankFilename_usesDocumentJpg() {
        Booking booking = confirmedBooking(LocalDate.now());
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), eq(property.getId()));

        MockMultipartFile file = new MockMultipartFile(
                "files", "", "image/png", new byte[]{9, 8, 7});

        List<String> urls = service.uploadIdDocumentsForManager(manager, booking.getId(), List.of(file));

        assertEquals(1, urls.size());
        assertTrue(urls.get(0).endsWith("_document.jpg"));
    }

    @Test
    void uploadIdDocuments_emptyList_rejected() {
        Booking booking = confirmedBooking(LocalDate.now());
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), eq(property.getId()));

        assertThrows(BusinessException.class,
                () -> service.uploadIdDocumentsForManager(manager, booking.getId(), List.of()));
    }

    @Test
    void uploadIdDocuments_moreThanThree_rejected() {
        Booking booking = confirmedBooking(LocalDate.now());
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), eq(property.getId()));

        List<MultipartFile> files = List.of(
                new MockMultipartFile("f", "a.jpg", "image/jpeg", new byte[]{1}),
                new MockMultipartFile("f", "b.jpg", "image/jpeg", new byte[]{1}),
                new MockMultipartFile("f", "c.jpg", "image/jpeg", new byte[]{1}),
                new MockMultipartFile("f", "d.jpg", "image/jpeg", new byte[]{1}));

        assertThrows(BusinessException.class,
                () -> service.uploadIdDocumentsForManager(manager, booking.getId(), files));
    }

    @Test
    void uploadIdDocuments_allEmptyFiles_rejected() {
        Booking booking = confirmedBooking(LocalDate.now());
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), eq(property.getId()));

        MockMultipartFile empty = new MockMultipartFile("f", "a.jpg", "image/jpeg", new byte[0]);

        assertThrows(BusinessException.class,
                () -> service.uploadIdDocumentsForManager(manager, booking.getId(), List.of(empty)));
    }

    @Test
    void uploadIdDocuments_nonImageContentType_rejected() {
        Booking booking = confirmedBooking(LocalDate.now());
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), eq(property.getId()));

        MockMultipartFile pdf = new MockMultipartFile(
                "f", "id.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThrows(BusinessException.class,
                () -> service.uploadIdDocumentsForManager(manager, booking.getId(), List.of(pdf)));
    }

    @Test
    void uploadIdDocuments_tooLarge_rejected() {
        Booking booking = confirmedBooking(LocalDate.now());
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), eq(property.getId()));

        byte[] big = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("f", "big.jpg", "image/jpeg", big);

        assertThrows(BusinessException.class,
                () -> service.uploadIdDocumentsForManager(manager, booking.getId(), List.of(file)));
    }

    @Test
    void checkIn_invalidIdDocumentUrl_rejected() {
        Booking booking = confirmedBooking(LocalDate.now());
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), eq(property.getId()));

        ManagerCheckInRequest req = checkInRequest(true);
        req.setIdDocumentUrls(List.of("https://evil.example/id.jpg"));

        assertThrows(BusinessException.class,
                () -> service.markAsCheckedInForManager(manager, booking.getId(), req));
    }

    @Test
    void checkIn_tooManyIdUrls_rejected() {
        Booking booking = confirmedBooking(LocalDate.now());
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        doNothing().when(scopeValidator).validateManagerAccess(eq(manager), eq(property.getId()));

        ManagerCheckInRequest req = checkInRequest(true);
        req.setIdDocumentUrls(List.of(
                "/uploads/a.jpg", "/uploads/b.jpg", "/uploads/c.jpg", "/uploads/d.jpg"));

        assertThrows(BusinessException.class,
                () -> service.markAsCheckedInForManager(manager, booking.getId(), req));
    }

    @Test
    void createBooking_deletedRoom_conflict() {
        room.setDeletedAt(LocalDateTime.now());
        room.setStatus(Room.Status.AVAILABLE);
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));

        assertThrows(ConflictException.class,
                () -> service.createBooking(createRequest(
                        LocalDate.now().plusDays(2), LocalDate.now().plusDays(4), 2), customer));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CreateBookingRequest createRequest(LocalDate in, LocalDate out, int guests) {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setRoomId(room.getId());
        req.setCheckInDate(in);
        req.setCheckOutDate(out);
        req.setGuestCount(guests);
        req.setSpecialRequests("quiet room");
        return req;
    }

    private Booking confirmedBooking(LocalDate checkIn) {
        Booking b = TestFixtures.booking(customer, room, Booking.Status.CONFIRMED);
        b.setCheckInDate(checkIn);
        b.setCheckOutDate(checkIn.plusDays(2));
        b.setGuestCount(2);
        return b;
    }

    private ManagerCheckInRequest checkInRequest(boolean remainingCollected) {
        ManagerCheckInRequest req = new ManagerCheckInRequest();
        req.setIdDocumentUrls(List.of("/uploads/bookings/id.jpg"));
        req.setKeyHandedOver(true);
        req.setRemainingCollected(remainingCollected);
        return req;
    }
}
