package com.homestay.service;

import com.homestay.config.VNPayConfig;
import com.homestay.entity.Booking;
import com.homestay.entity.DamageReport;
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

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentVnpayReturnTest {

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
    private User customer;
    private Booking booking;
    private Property property;
    private Room room;

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
        customer = TestFixtures.user(User.Role.CUSTOMER);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
        booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_DEPOSIT);
        org.mockito.Mockito.lenient()
                .doNothing()
                .when(invoiceStatusSyncService)
                .syncFromPaymentEntity(any());
    }

    @Test
    void completeVnpayReturn_deposit_confirmsBooking_andContract() {
        Payment payment = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PENDING);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.completeVnpayReturn(payment.getId(), true, "00");

        assertEquals(Payment.Status.PAID, result.getStatus());
        assertEquals(Booking.Status.CONFIRMED, booking.getStatus());
        verify(contractService).autoGenerateAndSendContract(booking.getId(), customer);
        verify(damageFeeSettlementService, never()).markDamageReportPaidForBooking(any());
        verify(activityLogService).log(
                eq(customer),
                eq(ActivityLogService.PAYMENT_CONFIRMED),
                eq("Payment"),
                eq(payment.getId()),
                any());
    }

    @Test
    void completeVnpayReturn_remaining_marksPaid_withoutReconfirm() {
        booking.setStatus(Booking.Status.CONFIRMED);
        Payment payment = TestFixtures.payment(booking, Payment.Type.REMAINING_BALANCE, Payment.Status.PENDING);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.completeVnpayReturn(payment.getId(), true, "00");

        assertEquals(Payment.Status.PAID, result.getStatus());
        assertEquals(Booking.Status.CONFIRMED, booking.getStatus());
        verify(contractService, never()).autoGenerateAndSendContract(any(), any());
        verify(damageFeeSettlementService, never()).markDamageReportPaidForBooking(any());
        verify(activityLogService).log(
                eq(customer), eq(ActivityLogService.PAYMENT_CONFIRMED), eq("Payment"), eq(payment.getId()), any());
    }

    @Test
    void completeVnpayReturn_damageFee_marksPaid_andSettles() {
        booking.setStatus(Booking.Status.PENDING_DAMAGE_PAYMENT);
        booking.setDamageFeeAmount(new BigDecimal("500000"));
        Payment payment = TestFixtures.payment(booking, Payment.Type.DAMAGE_FEE, Payment.Status.PENDING);
        payment.setAmount(booking.getDamageFeeAmount());
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(damageReportRepository.findFirstByBooking_IdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(Optional.empty());

        Payment result = service.completeVnpayReturn(payment.getId(), true, "00");

        assertEquals(Payment.Status.PAID, result.getStatus());
        verify(damageFeeSettlementService).markDamageReportPaidForBooking(booking.getId());
        verify(contractService, never()).autoGenerateAndSendContract(any(), any());
        verify(activityLogService).log(
                eq(customer), eq(ActivityLogService.PAYMENT_CONFIRMED), eq("Payment"), eq(payment.getId()), any());
    }

    @Test
    void completeVnpayReturn_damageFee_disputed_blocked() {
        booking.setStatus(Booking.Status.PENDING_DAMAGE_PAYMENT);
        booking.setDamageFeeAmount(new BigDecimal("500000"));
        Payment payment = TestFixtures.payment(booking, Payment.Type.DAMAGE_FEE, Payment.Status.PENDING);
        DamageReport disputed = TestFixtures.damageReport(
                booking,
                TestFixtures.inspection(booking, property, room),
                new BigDecimal("500000"),
                false);
        disputed.setStatus(DamageReport.Status.DISPUTED);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(damageReportRepository.findFirstByBooking_IdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(Optional.of(disputed));

        assertThrows(BusinessException.class,
                () -> service.completeVnpayReturn(payment.getId(), true, "00"));
        verify(damageFeeSettlementService, never()).markDamageReportPaidForBooking(any());
    }

    @Test
    void completeVnpayReturn_idempotent_whenAlreadyPaid() {
        Payment payment = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID);
        booking.setStatus(Booking.Status.CONFIRMED);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        Payment result = service.completeVnpayReturn(payment.getId(), true, "00");

        assertSame(payment, result);
        verify(paymentRepository, never()).save(any());
        verify(contractService, never()).autoGenerateAndSendContract(any(), any());
        verify(bookingRepository, never()).save(any());
        verify(activityLogService, never()).log(any(), any(), any(), any(), any());
    }

    @Test
    void completeVnpayReturn_failure_marksFailed() {
        Payment payment = TestFixtures.payment(booking, Payment.Type.REMAINING_BALANCE, Payment.Status.PENDING);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.completeVnpayReturn(payment.getId(), false, "24", "13999999");

        assertEquals(Payment.Status.FAILED, result.getStatus());
        assertEquals("13999999", result.getGatewayTransactionId());
        verify(damageFeeSettlementService, never()).markDamageReportPaidForBooking(any(UUID.class));
        verify(contractService, never()).autoGenerateAndSendContract(any(), any());
        verify(paymentRepository, times(1)).save(payment);
        verify(activityLogService).log(
                eq(customer), eq(ActivityLogService.PAYMENT_FAILED), eq("Payment"), eq(payment.getId()), any());
    }

    @Test
    void completeVnpayReturn_success_storesGatewayTransactionId() {
        Payment payment = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PENDING);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.completeVnpayReturn(payment.getId(), true, "00", "14123456");

        assertEquals("14123456", result.getGatewayTransactionId());
        assertEquals("00", result.getGatewayResponseCode());
    }
}
