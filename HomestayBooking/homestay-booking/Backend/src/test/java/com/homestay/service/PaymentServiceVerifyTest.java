package com.homestay.service;

import com.homestay.config.VNPayConfig;
import com.homestay.dto.request.PaymentVerificationRequest;
import com.homestay.entity.Booking;
import com.homestay.entity.Payment;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.PaymentRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Manual CK verify is deferred (TD-011 / Spec Sync VNPay-only).
 * Endpoint/service must reject until Spec Sync re-opens bank transfer.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceVerifyTest {

    @Mock PaymentRepository paymentRepository;
    @Mock BookingRepository bookingRepository;
    @Mock ContractService contractService;
    @Mock VNPayService vnPayService;
    @Mock VNPayConfig vnPayConfig;
    @Mock ReportPropertyScopeValidator scopeValidator;
    @Mock ManagerPropertyAssignmentRepository assignmentRepository;
    @Mock DamageFeeSettlementService damageFeeSettlementService;
    @Mock ActivityLogService activityLogService;
    @Mock com.homestay.repository.DamageReportRepository damageReportRepository;
    @Mock com.homestay.repository.InvoiceRepository invoiceRepository;
    @Mock InvoiceStatusSyncService invoiceStatusSyncService;

    private PaymentService service;
    private User manager;
    private User customer;
    private Payment payment;

    @BeforeEach
    void setUp() {
        service = new PaymentService(
                paymentRepository,
                bookingRepository,
                contractService,
                vnPayService,
                vnPayConfig,
                scopeValidator,
                assignmentRepository,
                damageFeeSettlementService,
                activityLogService,
                damageReportRepository,
                invoiceRepository,
                invoiceStatusSyncService);
        manager = TestFixtures.user(User.Role.MANAGER);
        customer = TestFixtures.user(User.Role.CUSTOMER);
        Property property = TestFixtures.property();
        Room room = TestFixtures.room(property);
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_DEPOSIT);
        payment = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PENDING);
    }

    @Test
    void verifyPayment_deferred_td011_rejectsManager() {
        PaymentVerificationRequest req = new PaymentVerificationRequest();
        req.setStatus(Payment.Status.PAID);
        req.setNote("Bank transfer verified with receipt");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.verifyPayment(payment.getId(), req, manager));
        assertTrue(ex.getMessage().contains("TD-011"));
    }

    @Test
    void verifyPayment_deferred_td011_rejectsCustomer() {
        PaymentVerificationRequest req = new PaymentVerificationRequest();
        req.setStatus(Payment.Status.PAID);
        req.setNote("ok");

        assertThrows(BusinessException.class,
                () -> service.verifyPayment(payment.getId(), req, customer));
    }
}
