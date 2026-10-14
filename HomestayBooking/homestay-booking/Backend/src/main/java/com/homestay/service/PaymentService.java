package com.homestay.service;

import com.homestay.config.VNPayConfig;
import com.homestay.dto.request.PaymentVerificationRequest;
import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.PaymentDetailResponse;
import com.homestay.dto.response.PaymentSummaryResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.DamageReport;
import com.homestay.entity.Invoice;
import com.homestay.entity.Payment;
import com.homestay.entity.Property;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ForbiddenException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.DamageReportRepository;
import com.homestay.repository.InvoiceRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final ContractService contractService;
    private final VNPayService vnPayService;
    private final VNPayConfig vnPayConfig;
    private final ReportPropertyScopeValidator scopeValidator;
    private final ManagerPropertyAssignmentRepository assignmentRepository;
    private final DamageFeeSettlementService damageFeeSettlementService;
    private final ActivityLogService activityLogService;
    private final DamageReportRepository damageReportRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceStatusSyncService invoiceStatusSyncService;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingRepository bookingRepository,
                          ContractService contractService,
                          VNPayService vnPayService,
                          VNPayConfig vnPayConfig,
                          ReportPropertyScopeValidator scopeValidator,
                          ManagerPropertyAssignmentRepository assignmentRepository,
                          DamageFeeSettlementService damageFeeSettlementService,
                          ActivityLogService activityLogService,
                          DamageReportRepository damageReportRepository,
                          InvoiceRepository invoiceRepository,
                          InvoiceStatusSyncService invoiceStatusSyncService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.contractService = contractService;
        this.vnPayService = vnPayService;
        this.vnPayConfig = vnPayConfig;
        this.scopeValidator = scopeValidator;
        this.assignmentRepository = assignmentRepository;
        this.damageFeeSettlementService = damageFeeSettlementService;
        this.activityLogService = activityLogService;
        this.damageReportRepository = damageReportRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceStatusSyncService = invoiceStatusSyncService;
    }

    @Transactional
    public Map<String, String> createVnpayPaymentUrl(UUID bookingId, String type, User currentUser) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking không tồn tại"));

        if (!booking.getCustomer().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Bạn không có quyền thanh toán cho booking này");
        }

        Payment.Type paymentType;
        try {
            paymentType = Payment.Type.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại thanh toán không hợp lệ");
        }

        BigDecimal amountBd;
        if (paymentType == Payment.Type.DEPOSIT) {
            if (booking.getStatus() != Booking.Status.PENDING_DEPOSIT) {
                throw new BusinessException("Booking này không ở trạng thái chờ đặt cọc");
            }
            if (booking.getHoldExpiresAt() != null
                    && booking.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
                throw new BusinessException("Payment window expired");
            }
            amountBd = booking.getDepositAmount();
        } else if (paymentType == Payment.Type.REMAINING_BALANCE) {
            if (booking.getStatus() != Booking.Status.CONFIRMED) {
                throw new BusinessException("Booking phải ở trạng thái CONFIRMED để thanh toán phần còn lại qua VNPay");
            }
            amountBd = booking.getRemainingAmount();
        } else if (paymentType == Payment.Type.DAMAGE_FEE) {
            if (booking.getStatus() != Booking.Status.PENDING_DAMAGE_PAYMENT) {
                throw new BusinessException("Booking không ở trạng thái chờ thanh toán phí hư hại");
            }
            assertDamageFeeNotDisputed(booking.getId());
            amountBd = booking.getDamageFeeAmount();
        } else {
            throw new IllegalArgumentException("Loại thanh toán không hợp lệ");
        }

        if (amountBd == null || amountBd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền thanh toán không hợp lệ");
        }

        List<Payment> existing = paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId());
        boolean alreadyPaid = existing.stream().anyMatch(p ->
                p.getType() == paymentType && p.getStatus() == Payment.Status.PAID);
        if (alreadyPaid) {
            throw new BusinessException("Khoản thanh toán này đã được thanh toán");
        }

        Payment payment = existing.stream()
                .filter(p -> p.getType() == paymentType && p.getStatus() == Payment.Status.PENDING)
                .findFirst()
                .orElse(null);

        boolean isNew = payment == null;
        if (isNew) {
            payment = new Payment();
            payment.setBooking(booking);
            payment.setCustomer(currentUser);
            payment.setType(paymentType);
            payment.setStatus(Payment.Status.PENDING);
        }
        payment.setMethod(Payment.Method.VNPAY);
        payment.setAmount(amountBd);
        linkInvoiceIfPresent(payment, booking, paymentType);
        payment = paymentRepository.save(payment);

        if (payment.getOrderRef() == null || payment.getOrderRef().isBlank()) {
            // FR-004: unique orderRef = payment UUID (also vnp_TxnRef / return paymentId)
            payment.setOrderRef(payment.getId().toString());
            payment = paymentRepository.save(payment);
        }

        if (isNew) {
            activityLogService.log(
                    currentUser,
                    ActivityLogService.PAYMENT_CREATED,
                    "Payment",
                    payment.getId(),
                    "Created PENDING " + paymentType + " orderRef=" + payment.getOrderRef());
        }
        if (payment.getInvoice() != null) {
            invoiceStatusSyncService.markPendingPayment(payment.getInvoice().getId());
        } else {
            invoiceStatusSyncService.syncFromPaymentEntity(payment);
        }

        long amount = amountBd.setScale(0, java.math.RoundingMode.HALF_UP).longValue();
        String orderInfo = "Thanh toan " + paymentType.name() + " cho Booking " + booking.getId();
        String paymentUrl = vnPayService.createOrder(
                amount, orderInfo, vnPayConfig.getVnp_ReturnUrl(), payment.getOrderRef());

        Map<String, String> result = new HashMap<>();
        result.put("paymentUrl", paymentUrl);
        result.put("paymentId", payment.getId().toString());
        result.put("orderRef", payment.getOrderRef());
        return result;
    }

    /**
     * Resolve payment for VNPay return: query paymentId (UUID) or vnp_TxnRef / orderRef.
     */
    @Transactional(readOnly = true)
    public UUID resolvePaymentIdForReturn(String paymentIdParam, String vnpTxnRef) {
        if (paymentIdParam != null && !paymentIdParam.isBlank()) {
            try {
                return UUID.fromString(paymentIdParam.trim());
            } catch (IllegalArgumentException ignored) {
                return paymentRepository.findByOrderRef(paymentIdParam.trim())
                        .map(Payment::getId)
                        .orElseThrow(() -> new ResourceNotFoundException("Payment không tồn tại"));
            }
        }
        if (vnpTxnRef != null && !vnpTxnRef.isBlank()) {
            try {
                return UUID.fromString(vnpTxnRef.trim());
            } catch (IllegalArgumentException ignored) {
                return paymentRepository.findByOrderRef(vnpTxnRef.trim())
                        .map(Payment::getId)
                        .orElseThrow(() -> new ResourceNotFoundException("Payment không tồn tại"));
            }
        }
        throw new ResourceNotFoundException("Payment không tồn tại");
    }

    /**
     * VNPay return handler (012 FR-004/005/006 + US6). Idempotent on already-PAID.
     */
    @Transactional
    public Payment completeVnpayReturn(UUID paymentId, boolean success, String gatewayResponseCode) {
        return completeVnpayReturn(paymentId, success, gatewayResponseCode, null);
    }

    @Transactional
    public Payment completeVnpayReturn(
            UUID paymentId, boolean success, String gatewayResponseCode, String gatewayTransactionId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment không tồn tại"));

        payment.setGatewayResponseCode(gatewayResponseCode);
        if (gatewayTransactionId != null && !gatewayTransactionId.isBlank()) {
            payment.setGatewayTransactionId(gatewayTransactionId.trim());
        }

        if (!success) {
            if (payment.getStatus() == Payment.Status.PENDING) {
                payment.setStatus(Payment.Status.FAILED);
                paymentRepository.save(payment);
                activityLogService.log(
                        payment.getCustomer(),
                        ActivityLogService.PAYMENT_FAILED,
                        "Payment",
                        payment.getId(),
                        "VNPay return failed for " + payment.getType()
                                + " (code=" + gatewayResponseCode + ")");
                invoiceStatusSyncService.syncFromPaymentEntity(payment);
            }
            return payment;
        }

        if (payment.getStatus() == Payment.Status.PAID) {
            return payment;
        }
        if (payment.getStatus() != Payment.Status.PENDING) {
            throw new BusinessException("Payment không ở trạng thái chờ thanh toán");
        }

        if (payment.getType() == Payment.Type.DAMAGE_FEE) {
            assertDamageFeeNotDisputed(payment.getBooking().getId());
        }

        payment.setStatus(Payment.Status.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
        activityLogService.log(
                payment.getCustomer(),
                ActivityLogService.PAYMENT_CONFIRMED,
                "Payment",
                payment.getId(),
                "VNPay paid " + payment.getType() + " amount=" + payment.getAmount()
                        + (gatewayTransactionId != null ? " txn=" + gatewayTransactionId : ""));
        invoiceStatusSyncService.syncFromPaymentEntity(payment);
        applyPaidSideEffects(payment);
        return payment;
    }

    private void linkInvoiceIfPresent(Payment payment, Booking booking, Payment.Type paymentType) {
        Invoice.Type invType = switch (paymentType) {
            case DEPOSIT -> Invoice.Type.DEPOSIT;
            case REMAINING_BALANCE -> Invoice.Type.REMAINING_BALANCE;
            default -> null;
        };
        if (invType == null) {
            return;
        }
        invoiceRepository.findByBooking_IdAndType(booking.getId(), invType)
                .ifPresent(payment::setInvoice);
    }

    private void applyPaidSideEffects(Payment payment) {
        Booking booking = payment.getBooking();
        if (payment.getType() == Payment.Type.DEPOSIT) {
            if (booking.getStatus() == Booking.Status.PENDING_DEPOSIT) {
                booking.setStatus(Booking.Status.CONFIRMED);
                bookingRepository.save(booking);
                try {
                    contractService.autoGenerateAndSendContract(booking.getId(), payment.getCustomer());
                } catch (Exception e) {
                    log.warn("Contract outbox after deposit failed for booking {}: {}",
                            booking.getId(), e.getMessage());
                }
            }
            return;
        }
        if (payment.getType() == Payment.Type.REMAINING_BALANCE) {
            // FR-006: update paid balance via payment record only — do not re-confirm booking
            return;
        }
        if (payment.getType() == Payment.Type.DAMAGE_FEE) {
            damageFeeSettlementService.markDamageReportPaidForBooking(booking.getId());
        }
    }

    /** 023 US6 scenario 3 — block DAMAGE_FEE while report is DISPUTED. */
    private void assertDamageFeeNotDisputed(UUID bookingId) {
        damageReportRepository.findFirstByBooking_IdOrderByCreatedAtDesc(bookingId).ifPresent(dr -> {
            if (dr.getStatus() == DamageReport.Status.DISPUTED) {
                throw new BusinessException(
                        "Phí hư hại đang bị tranh chấp — tạm khóa thanh toán đến khi Admin xử lý");
            }
        });
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentSummaryResponse> getMyPayments(User currentUser, int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size);
        Payment.Status paymentStatus = parseStatus(status);
        Page<Payment> result = paymentStatus == null
                ? paymentRepository.findByCustomerIdOrderByCreatedAtDesc(currentUser.getId(), pageable)
                : paymentRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(
                        currentUser.getId(), paymentStatus, pageable);

        return new PageResponse<>(
                result.getContent().stream().map(PaymentSummaryResponse::fromEntity).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentSummaryResponse> getAllPayments(int page, int size, String status, String search, String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        Payment.Status paymentStatus = parseStatus(status);
        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<Payment> result = paymentRepository.findAllWithFilters(paymentStatus, searchParam, pageable);

        return new PageResponse<>(
                result.getContent().stream().map(PaymentSummaryResponse::fromEntity).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    /** SCR-36 — Manager payment list scoped to assigned properties (v1). */
    @Transactional(readOnly = true)
    public PageResponse<PaymentSummaryResponse> getPaymentsForManagerScoped(
            User manager,
            String propertyIdStr,
            String status,
            String type,
            String method,
            String search,
            int page,
            int size,
            String sort
    ) {
        Pageable pageable = buildPageable(page, size, sort);

        List<UUID> propertyIds;
        if (propertyIdStr != null && !propertyIdStr.isBlank()) {
            UUID propertyId = UUID.fromString(propertyIdStr.trim());
            scopeValidator.validateManagerAccess(manager, propertyId);
            propertyIds = List.of(propertyId);
        } else {
            propertyIds = assignmentRepository.findActivePropertiesByManagerId(manager.getId())
                    .stream()
                    .map(Property::getId)
                    .toList();
        }

        if (propertyIds.isEmpty()) {
            return new PageResponse<>(List.of(), page, size, 0, 0);
        }

        Payment.Status paymentStatus = parseStatus(status);
        Payment.Type paymentType = parseType(type);
        Payment.Method paymentMethod = parseMethod(method);
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<Payment> result = paymentRepository.findForManagerWithFilters(
                propertyIds, paymentStatus, paymentType, paymentMethod, cleanSearch, pageable);

        return new PageResponse<>(
                result.getContent().stream().map(PaymentSummaryResponse::fromEntity).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PaymentDetailResponse getPaymentDetail(UUID id, User currentUser) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment không tồn tại"));

        boolean isManager = currentUser.getRole() == User.Role.MANAGER;
        if (isManager) {
            UUID propertyId = payment.getBooking().getRoom().getProperty().getId();
            scopeValidator.validateManagerAccess(currentUser, propertyId);
        } else if (!payment.getCustomer().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Không có quyền xem thanh toán này");
        }

        return PaymentDetailResponse.fromEntity(payment);
    }

    /**
     * Manual bank-transfer verify (SCR-37) — deferred TD-011 (VNPay-only edu cut).
     * Kept for API compatibility; rejects until Spec Sync re-opens CK flow.
     */
    @Transactional
    public PaymentDetailResponse verifyPayment(UUID id, PaymentVerificationRequest request, User currentUser) {
        throw new BusinessException(
                "Xác minh chuyển khoản thủ công đang tạm dừng (TD-011 / Spec Sync). Thanh toán qua VNPay tự động xác nhận.");
    }

    private Pageable buildPageable(int page, int size, String sort) {
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        List<String> allowed = Arrays.asList("createdAt", "amount", "status", "type");
        if (!allowed.contains(field)) {
            field = "createdAt";
        }
        return PageRequest.of(page, size, Sort.by(direction, field));
    }

    private Payment.Status parseStatus(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) return null;
        try {
            return Payment.Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Payment.Type parseType(String type) {
        if (type == null || type.isBlank()) return null;
        try {
            return Payment.Type.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Payment.Method parseMethod(String method) {
        if (method == null || method.isBlank()) return null;
        try {
            return Payment.Method.valueOf(method.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
