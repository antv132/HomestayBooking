package com.homestay.service;

import com.homestay.config.VNPayConfig;
import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.PaymentDetailResponse;
import com.homestay.dto.response.PaymentSummaryResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.DamageReport;
import com.homestay.entity.Payment;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ForbiddenException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.DamageReportRepository;
import com.homestay.repository.InvoiceRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.PaymentRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock BookingRepository bookingRepository;
    @Mock ContractService contractService;
    @Mock VNPayService vnPayService;
    @Mock VNPayConfig vnPayConfig;
    @Mock ReportPropertyScopeValidator scopeValidator;
    @Mock ManagerPropertyAssignmentRepository assignmentRepository;
    @Mock DamageFeeSettlementService damageFeeSettlementService;
    @Mock ActivityLogService activityLogService;
    @Mock DamageReportRepository damageReportRepository;
    @Mock com.homestay.repository.InvoiceRepository invoiceRepository;
    @Mock InvoiceStatusSyncService invoiceStatusSyncService;

    private PaymentService service;
    private User customer;
    private User otherCustomer;
    private User manager;
    private Property property;
    private Room room;
    private Booking booking;

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
        otherCustomer = TestFixtures.user(User.Role.CUSTOMER);
        manager = TestFixtures.user(User.Role.MANAGER);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
        booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_DEPOSIT);
        org.mockito.Mockito.lenient()
                .when(invoiceRepository.findByBooking_IdAndType(any(), any()))
                .thenReturn(Optional.empty());
        org.mockito.Mockito.lenient()
                .doNothing()
                .when(invoiceStatusSyncService)
                .syncFromPaymentEntity(any());
        org.mockito.Mockito.lenient()
                .doNothing()
                .when(invoiceStatusSyncService)
                .markPendingPayment(any());
    }

    // ── createVnpayPaymentUrl ──────────────────────────────────────────────

    @Test
    void createVnpayPaymentUrl_deposit_createsPendingAndReturnsUrl() {
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(Collections.emptyList());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
            }
            return p;
        });
        when(vnPayConfig.getVnp_ReturnUrl()).thenReturn("http://localhost/return");
        when(vnPayService.createOrder(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn("https://vnpay.example/pay");

        Map<String, String> result = service.createVnpayPaymentUrl(
                booking.getId(), "DEPOSIT", customer);

        assertEquals("https://vnpay.example/pay", result.get("paymentUrl"));
        assertNotNull(result.get("paymentId"));
        assertNotNull(result.get("orderRef"));
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        Payment saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(Payment.Type.DEPOSIT, saved.getType());
        assertEquals(Payment.Status.PENDING, saved.getStatus());
        assertEquals(Payment.Method.VNPAY, saved.getMethod());
        assertEquals(booking.getDepositAmount(), saved.getAmount());
        assertEquals(saved.getId().toString(), saved.getOrderRef());
        verify(vnPayService).createOrder(
                eq(4_000_000L), anyString(), eq("http://localhost/return"), eq(saved.getOrderRef()));
        verify(activityLogService).log(
                eq(customer),
                eq(ActivityLogService.PAYMENT_CREATED),
                eq("Payment"),
                eq(saved.getId()),
                anyString());
    }

    @Test
    void createVnpayPaymentUrl_remaining_whenConfirmed_returnsUrl() {
        booking.setStatus(Booking.Status.CONFIRMED);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(Collections.emptyList());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(vnPayConfig.getVnp_ReturnUrl()).thenReturn("http://localhost/return");
        when(vnPayService.createOrder(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn("https://vnpay.example/remaining");

        Map<String, String> result = service.createVnpayPaymentUrl(
                booking.getId(), "REMAINING_BALANCE", customer);

        assertEquals("https://vnpay.example/remaining", result.get("paymentUrl"));
        assertNotNull(result.get("paymentId"));
        verify(vnPayService).createOrder(
                eq(6_000_000L), anyString(), anyString(), anyString());
    }

    @Test
    void createVnpayPaymentUrl_remaining_whenCheckedIn_blocked() {
        booking.setStatus(Booking.Status.CHECKED_IN);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThrows(BusinessException.class, () ->
                service.createVnpayPaymentUrl(booking.getId(), "REMAINING_BALANCE", customer));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createVnpayPaymentUrl_damageFee_returnsUrl() {
        booking.setStatus(Booking.Status.PENDING_DAMAGE_PAYMENT);
        booking.setDamageFeeAmount(new BigDecimal("750000"));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(Collections.emptyList());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(vnPayConfig.getVnp_ReturnUrl()).thenReturn("http://localhost/return");
        when(vnPayService.createOrder(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn("https://vnpay.example/damage");

        Map<String, String> result = service.createVnpayPaymentUrl(
                booking.getId(), "DAMAGE_FEE", customer);

        assertEquals("https://vnpay.example/damage", result.get("paymentUrl"));
        verify(vnPayService).createOrder(eq(750_000L), anyString(), anyString(), anyString());
    }

    @Test
    void createVnpayPaymentUrl_damageFee_disputed_blocked() {
        booking.setStatus(Booking.Status.PENDING_DAMAGE_PAYMENT);
        booking.setDamageFeeAmount(new BigDecimal("750000"));
        DamageReport disputed = TestFixtures.damageReport(
                booking,
                TestFixtures.inspection(booking, property, room),
                new BigDecimal("750000"),
                false);
        disputed.setStatus(DamageReport.Status.DISPUTED);

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(damageReportRepository.findFirstByBooking_IdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(Optional.of(disputed));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createVnpayPaymentUrl(booking.getId(), "DAMAGE_FEE", customer));
        assertTrue(ex.getMessage().toLowerCase().contains("tranh chấp"));
        verify(vnPayService, never()).createOrder(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void createVnpayPaymentUrl_reusesExistingPendingPayment() {
        Payment pending = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PENDING);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vnPayConfig.getVnp_ReturnUrl()).thenReturn("http://localhost/return");
        when(vnPayService.createOrder(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn("https://vnpay.example/reuse");

        Map<String, String> result = service.createVnpayPaymentUrl(
                booking.getId(), "DEPOSIT", customer);

        assertEquals("https://vnpay.example/reuse", result.get("paymentUrl"));
        verify(vnPayService).createOrder(anyLong(), anyString(), anyString(), eq(pending.getId().toString()));
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertEquals(pending.getId(), captor.getValue().getId());
    }

    @Test
    void createVnpayPaymentUrl_bookingNotFound_throws() {
        UUID missing = UUID.randomUUID();
        when(bookingRepository.findById(missing)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.createVnpayPaymentUrl(missing, "DEPOSIT", customer));
    }

    @Test
    void createVnpayPaymentUrl_otherCustomer_throwsForbidden() {
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThrows(ForbiddenException.class,
                () -> service.createVnpayPaymentUrl(booking.getId(), "DEPOSIT", otherCustomer));
    }

    @Test
    void createVnpayPaymentUrl_invalidType_throwsIllegalArgument() {
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThrows(IllegalArgumentException.class,
                () -> service.createVnpayPaymentUrl(booking.getId(), "NOT_A_TYPE", customer));
    }

    @Test
    void createVnpayPaymentUrl_deposit_wrongStatus_throwsBusiness() {
        booking.setStatus(Booking.Status.CONFIRMED);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createVnpayPaymentUrl(booking.getId(), "DEPOSIT", customer));
        assertTrue(ex.getMessage().contains("chờ đặt cọc"));
    }

    @Test
    void createVnpayPaymentUrl_deposit_holdExpired_throwsBusiness() {
        booking.setHoldExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createVnpayPaymentUrl(booking.getId(), "DEPOSIT", customer));
        assertEquals("Payment window expired", ex.getMessage());
    }

    @Test
    void createVnpayPaymentUrl_remaining_wrongStatus_throwsBusiness() {
        booking.setStatus(Booking.Status.PENDING_DEPOSIT);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThrows(BusinessException.class,
                () -> service.createVnpayPaymentUrl(booking.getId(), "REMAINING_BALANCE", customer));
    }

    @Test
    void createVnpayPaymentUrl_damageFee_wrongStatus_throwsBusiness() {
        booking.setStatus(Booking.Status.CHECKED_OUT);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThrows(BusinessException.class,
                () -> service.createVnpayPaymentUrl(booking.getId(), "DAMAGE_FEE", customer));
    }

    @Test
    void createVnpayPaymentUrl_zeroAmount_throwsBusiness() {
        booking.setDepositAmount(BigDecimal.ZERO);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createVnpayPaymentUrl(booking.getId(), "DEPOSIT", customer));
        assertTrue(ex.getMessage().contains("không hợp lệ"));
    }

    @Test
    void createVnpayPaymentUrl_nullAmount_throwsBusiness() {
        booking.setStatus(Booking.Status.PENDING_DAMAGE_PAYMENT);
        booking.setDamageFeeAmount(null);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThrows(BusinessException.class,
                () -> service.createVnpayPaymentUrl(booking.getId(), "DAMAGE_FEE", customer));
    }

    @Test
    void createVnpayPaymentUrl_alreadyPaid_throwsBusiness() {
        Payment paid = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()))
                .thenReturn(List.of(paid));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createVnpayPaymentUrl(booking.getId(), "DEPOSIT", customer));
        assertTrue(ex.getMessage().contains("đã được thanh toán"));
    }

    // ── completeVnpayReturn extras ─────────────────────────────────────────

    @Test
    void completeVnpayReturn_paymentNotFound_throws() {
        UUID missing = UUID.randomUUID();
        when(paymentRepository.findById(missing)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.completeVnpayReturn(missing, true, "00"));
    }

    @Test
    void completeVnpayReturn_success_whenFailed_throwsBusiness() {
        Payment payment = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.FAILED);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        assertThrows(BusinessException.class,
                () -> service.completeVnpayReturn(payment.getId(), true, "00"));
    }

    @Test
    void completeVnpayReturn_failure_whenAlreadyPaid_leavesPaid() {
        Payment payment = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        Payment result = service.completeVnpayReturn(payment.getId(), false, "24");

        assertEquals(Payment.Status.PAID, result.getStatus());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void completeVnpayReturn_deposit_contractException_stillConfirms() {
        Payment payment = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PENDING);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("smtp down"))
                .when(contractService).autoGenerateAndSendContract(booking.getId(), customer);

        Payment result = service.completeVnpayReturn(payment.getId(), true, "00");

        assertEquals(Payment.Status.PAID, result.getStatus());
        assertEquals(Booking.Status.CONFIRMED, booking.getStatus());
    }

    @Test
    void completeVnpayReturn_deposit_whenBookingAlreadyConfirmed_skipsStatusChange() {
        booking.setStatus(Booking.Status.CONFIRMED);
        Payment payment = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PENDING);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.completeVnpayReturn(payment.getId(), true, "00");

        assertEquals(Payment.Status.PAID, result.getStatus());
        verify(bookingRepository, never()).save(any());
        verify(contractService, never()).autoGenerateAndSendContract(any(), any());
    }

    // ── getMyPayments / getAllPayments ─────────────────────────────────────

    @Test
    void getMyPayments_withoutStatus_listsCustomerPayments() {
        Payment payment = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID);
        when(paymentRepository.findByCustomerIdOrderByCreatedAtDesc(eq(customer.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payment), PageRequest.of(0, 10), 1));

        PageResponse<PaymentSummaryResponse> page = service.getMyPayments(customer, 0, 10, null);

        assertEquals(1, page.getContent().size());
        assertEquals(payment.getId(), page.getContent().get(0).getId());
        verify(paymentRepository).findByCustomerIdOrderByCreatedAtDesc(eq(customer.getId()), any(Pageable.class));
    }

    @Test
    void getMyPayments_withStatus_filters() {
        Payment payment = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PENDING);
        when(paymentRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(
                eq(customer.getId()), eq(Payment.Status.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payment), PageRequest.of(0, 5), 1));

        PageResponse<PaymentSummaryResponse> page = service.getMyPayments(customer, 0, 5, "PENDING");

        assertEquals(1, page.getTotalElements());
        verify(paymentRepository).findByCustomerIdAndStatusOrderByCreatedAtDesc(
                eq(customer.getId()), eq(Payment.Status.PENDING), any(Pageable.class));
    }

    @Test
    void getMyPayments_invalidStatus_treatedAsAll() {
        when(paymentRepository.findByCustomerIdOrderByCreatedAtDesc(eq(customer.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        service.getMyPayments(customer, 0, 10, "ALL");
        service.getMyPayments(customer, 0, 10, "bogus");

        verify(paymentRepository, org.mockito.Mockito.times(2))
                .findByCustomerIdOrderByCreatedAtDesc(eq(customer.getId()), any(Pageable.class));
    }

    @Test
    void getAllPayments_withFiltersAndSort_returnsPage() {
        Payment payment = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID);
        when(paymentRepository.findAllWithFilters(
                eq(Payment.Status.PAID), eq("alice"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payment), PageRequest.of(0, 10), 1));

        PageResponse<PaymentSummaryResponse> page =
                service.getAllPayments(0, 10, "PAID", " alice ", "amount,asc");

        assertEquals(1, page.getContent().size());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(paymentRepository).findAllWithFilters(
                eq(Payment.Status.PAID), eq("alice"), pageableCaptor.capture());
        assertEquals(Sort.Direction.ASC, pageableCaptor.getValue().getSort().getOrderFor("amount").getDirection());
    }

    @Test
    void getAllPayments_blankSortAndSearch_defaultsCreatedAtDesc() {
        when(paymentRepository.findAllWithFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.getAllPayments(0, 20, null, "  ", " ");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(paymentRepository).findAllWithFilters(isNull(), isNull(), pageableCaptor.capture());
        assertEquals(Sort.Direction.DESC,
                pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void getAllPayments_disallowedSortField_fallsBackToCreatedAt() {
        when(paymentRepository.findAllWithFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        service.getAllPayments(0, 10, null, null, "hackedField,desc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(paymentRepository).findAllWithFilters(isNull(), isNull(), pageableCaptor.capture());
        assertNotNull(pageableCaptor.getValue().getSort().getOrderFor("createdAt"));
    }

    // ── getPaymentsForManagerScoped ────────────────────────────────────────

    @Test
    void getPaymentsForManagerScoped_withPropertyId_validatesScope() {
        Payment payment = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID);
        doNothing().when(scopeValidator).validateManagerAccess(manager, property.getId());
        when(paymentRepository.findForManagerWithFilters(
                eq(List.of(property.getId())),
                eq(Payment.Status.PAID),
                eq(Payment.Type.DEPOSIT),
                eq(Payment.Method.VNPAY),
                eq("search"),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payment), PageRequest.of(0, 10), 1));

        PageResponse<PaymentSummaryResponse> page = service.getPaymentsForManagerScoped(
                manager,
                property.getId().toString(),
                "PAID",
                "DEPOSIT",
                "VNPAY",
                "search",
                0,
                10,
                "status,desc");

        assertEquals(1, page.getContent().size());
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
    }

    @Test
    void getPaymentsForManagerScoped_withoutProperty_usesAssignments() {
        when(assignmentRepository.findActivePropertiesByManagerId(manager.getId()))
                .thenReturn(List.of(property));
        when(paymentRepository.findForManagerWithFilters(
                eq(List.of(property.getId())),
                isNull(), isNull(), isNull(), isNull(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        PageResponse<PaymentSummaryResponse> page = service.getPaymentsForManagerScoped(
                manager, null, null, "badType", "badMethod", null, 0, 10, null);

        assertEquals(0, page.getTotalElements());
        verify(assignmentRepository).findActivePropertiesByManagerId(manager.getId());
    }

    @Test
    void getPaymentsForManagerScoped_noAssignedProperties_returnsEmpty() {
        when(assignmentRepository.findActivePropertiesByManagerId(manager.getId()))
                .thenReturn(List.of());

        PageResponse<PaymentSummaryResponse> page = service.getPaymentsForManagerScoped(
                manager, "  ", null, null, null, null, 1, 20, null);

        assertEquals(0, page.getTotalElements());
        assertEquals(1, page.getPage());
        assertEquals(20, page.getSize());
        verify(paymentRepository, never()).findForManagerWithFilters(
                anyList(), any(), any(), any(), any(), any());
    }

    // ── getPaymentDetail ───────────────────────────────────────────────────

    @Test
    void getPaymentDetail_customerOwner_returnsDetail() {
        Payment payment = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        PaymentDetailResponse detail = service.getPaymentDetail(payment.getId(), customer);

        assertEquals(payment.getId(), detail.getId());
        assertEquals(booking.getId(), detail.getBookingId());
    }

    @Test
    void getPaymentDetail_manager_validatesScope() {
        Payment payment = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        doNothing().when(scopeValidator).validateManagerAccess(manager, property.getId());

        PaymentDetailResponse detail = service.getPaymentDetail(payment.getId(), manager);

        assertEquals(payment.getId(), detail.getId());
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
    }

    @Test
    void getPaymentDetail_otherCustomer_throwsForbidden() {
        Payment payment = TestFixtures.payment(booking, Payment.Type.DEPOSIT, Payment.Status.PAID);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));

        assertThrows(ForbiddenException.class,
                () -> service.getPaymentDetail(payment.getId(), otherCustomer));
    }

    @Test
    void getPaymentDetail_notFound_throws() {
        UUID missing = UUID.randomUUID();
        when(paymentRepository.findById(missing)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getPaymentDetail(missing, customer));
    }
}
