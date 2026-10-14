package com.homestay.service;

import com.homestay.dto.request.ManagerCheckInRequest;
import com.homestay.entity.Booking;
import com.homestay.entity.Payment;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceCheckInTest {

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
                10L,
                24L);
        lenient().when(adminSettingsService.getDepositPercentage()).thenReturn(new java.math.BigDecimal("0.40"));
        manager = TestFixtures.user(User.Role.MANAGER);
        customer = TestFixtures.user(User.Role.CUSTOMER);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
        booking = TestFixtures.booking(customer, room, Booking.Status.CONFIRMED);
        booking.setRemainingAmount(new BigDecimal("6000000"));
    }

    @Test
    void checkIn_remainingUnpaidWithoutDeskCollection_denied() {
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId())).thenReturn(List.of(
                TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID)
        ));

        ManagerCheckInRequest req = new ManagerCheckInRequest();
        req.setIdDocumentUrls(List.of("/uploads/bookings/" + booking.getId() + "/front.jpg"));
        req.setKeyHandedOver(true);
        req.setRemainingCollected(false);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.markAsCheckedInForManager(manager, booking.getId(), req));
        assertTrue(ex.getMessage().contains("Chưa thu phần còn lại")
                || ex.getMessage().toUpperCase().contains("CHECKIN_DENIED")
                || ex.getMessage().toLowerCase().contains("remaining"));
    }
}
