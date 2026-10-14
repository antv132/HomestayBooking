package com.homestay.service;

import com.homestay.dto.request.ManagerCheckInRequest;
import com.homestay.dto.request.ManagerCheckOutRequest;
import com.homestay.dto.request.ModifyBookingRequest;
import com.homestay.dto.response.BookingDetailResponse;
import com.homestay.dto.response.BookingSummaryResponse;
import com.homestay.dto.response.CancellationPreviewResponse;
import com.homestay.dto.response.ManagerBookingDetailResponse;
import com.homestay.dto.response.ModifyBookingResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.BookingCheckVerification;
import com.homestay.entity.Contract;
import com.homestay.entity.DamageReport;
import com.homestay.entity.Invoice;
import com.homestay.entity.Payment;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.RoomInspection;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ConflictException;
import com.homestay.exception.ForbiddenException;
import com.homestay.repository.BookingCheckVerificationRepository;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.ContractRepository;
import com.homestay.repository.DamageReportRepository;
import com.homestay.repository.InvoiceRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.PaymentRepository;
import com.homestay.repository.RoomInspectionRepository;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookingService {

    private static final java.util.Set<Room.Status> BOOKING_BLOCKED_ROOM_STATUSES = java.util.Set.of(
            Room.Status.MAINTENANCE,
            Room.Status.OUT_OF_SERVICE,
            Room.Status.PENDING_CLEANING,
            Room.Status.CLEANING_IN_PROGRESS
    );

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final com.homestay.repository.RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;
    private final com.homestay.repository.ReviewRepository reviewRepository;
    private final ReportPropertyScopeValidator scopeValidator;
    private final ManagerPropertyAssignmentRepository assignmentRepository;
    private final RoomInspectionRepository roomInspectionRepository;
    private final BookingCheckVerificationRepository checkVerificationRepository;
    private final HousekeepingTaskService housekeepingTaskService;
    private final ContractRepository contractRepository;
    private final DamageReportRepository damageReportRepository;
    private final com.homestay.repository.RoomStatusBlockRepository roomStatusBlockRepository;
    private final InvoiceIssuanceService invoiceIssuanceService;
    private final InvoiceCancellationService invoiceCancellationService;
    private final InvoiceAdjustmentService invoiceAdjustmentService;
    private final InvoiceQueryService invoiceQueryService;
    private final InvoiceStatusSyncService invoiceStatusSyncService;
    private final InvoiceRepository invoiceRepository;
    private final AdminSettingsService adminSettingsService;
    private final long holdTimeoutMinutes;
    private final long noShowAfterHours;

    private static final long MAX_ID_DOC_BYTES = 5L * 1024 * 1024;
    private static final int MANAGER_CANCEL_REFUND_PERCENT = 100;

    public BookingService(BookingRepository bookingRepository,
                          NotificationService notificationService,
                          com.homestay.repository.RoomRepository roomRepository,
                          PaymentRepository paymentRepository,
                          com.homestay.repository.ReviewRepository reviewRepository,
                          ReportPropertyScopeValidator scopeValidator,
                          ManagerPropertyAssignmentRepository assignmentRepository,
                          RoomInspectionRepository roomInspectionRepository,
                          BookingCheckVerificationRepository checkVerificationRepository,
                          HousekeepingTaskService housekeepingTaskService,
                          ContractRepository contractRepository,
                          DamageReportRepository damageReportRepository,
                          com.homestay.repository.RoomStatusBlockRepository roomStatusBlockRepository,
                          InvoiceIssuanceService invoiceIssuanceService,
                          InvoiceCancellationService invoiceCancellationService,
                          InvoiceAdjustmentService invoiceAdjustmentService,
                          InvoiceQueryService invoiceQueryService,
                          InvoiceStatusSyncService invoiceStatusSyncService,
                          InvoiceRepository invoiceRepository,
                          AdminSettingsService adminSettingsService,
                          @org.springframework.beans.factory.annotation.Value("${app.booking.hold-timeout-minutes:10}") long holdTimeoutMinutes,
                          @org.springframework.beans.factory.annotation.Value("${app.booking.no-show-after-hours:24}") long noShowAfterHours) {
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
        this.roomRepository = roomRepository;
        this.paymentRepository = paymentRepository;
        this.reviewRepository = reviewRepository;
        this.scopeValidator = scopeValidator;
        this.assignmentRepository = assignmentRepository;
        this.roomInspectionRepository = roomInspectionRepository;
        this.checkVerificationRepository = checkVerificationRepository;
        this.housekeepingTaskService = housekeepingTaskService;
        this.contractRepository = contractRepository;
        this.damageReportRepository = damageReportRepository;
        this.roomStatusBlockRepository = roomStatusBlockRepository;
        this.invoiceIssuanceService = invoiceIssuanceService;
        this.invoiceCancellationService = invoiceCancellationService;
        this.invoiceAdjustmentService = invoiceAdjustmentService;
        this.invoiceQueryService = invoiceQueryService;
        this.invoiceStatusSyncService = invoiceStatusSyncService;
        this.invoiceRepository = invoiceRepository;
        this.adminSettingsService = adminSettingsService;
        this.holdTimeoutMinutes = holdTimeoutMinutes > 0 ? holdTimeoutMinutes : 10;
        this.noShowAfterHours = noShowAfterHours > 0 ? noShowAfterHours : 24;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PageResponse<BookingSummaryResponse> getMyBookings(
            User currentUser,
            int page,
            int size,
            String status,
            String sort
    ) {
        if (currentUser.getRole() != User.Role.CUSTOMER) {
            throw new ForbiddenException("Chỉ khách hàng mới có danh sách đặt phòng");
        }

        Pageable pageable = buildPageable(page, size, sort);
        Page<Booking> result;

        if (status != null && !status.isBlank()) {
            Booking.Status bookingStatus = Booking.Status.valueOf(status.trim().toUpperCase());
            result = bookingRepository.findByCustomerIdAndStatus(
                    currentUser.getId(), bookingStatus, pageable);
        } else {
            result = bookingRepository.findByCustomerId(currentUser.getId(), pageable);
        }

        return new PageResponse<>(
                result.getContent().stream().map(booking -> {
                    boolean isReviewed = reviewRepository.existsByBooking_Id(booking.getId());
                    return BookingSummaryResponse.fromEntity(booking, isReviewed);
                }).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public static Pageable buildPageable(int page, int size, String sort) {
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        List<String> allowed = Arrays.asList("createdAt", "checkInDate", "totalAmount", "status");
        if (!allowed.contains(field)) {
            field = "createdAt";
        }
        return PageRequest.of(page, size, Sort.by(direction, field));
    }
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PageResponse<BookingSummaryResponse> getAllBookings(
            int page,
            int size,
            String status,
            String search,
            String sort
    ) {
        Pageable pageable = buildPageable(page, size, sort);
        Booking.Status bookingStatus = (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL"))
                ? Booking.Status.valueOf(status.trim().toUpperCase()) : null;

        Page<Booking> result = bookingRepository.findAllWithFilters(bookingStatus, search, pageable);

        return new PageResponse<>(
                result.getContent().stream().map(booking -> {
                    boolean isReviewed = reviewRepository.existsByBooking_Id(booking.getId());
                    return BookingSummaryResponse.fromEntity(booking, isReviewed);
                }).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    /** SCR-34 — Manager booking list scoped to assigned properties (v1). */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PageResponse<BookingSummaryResponse> getBookingsForManagerScoped(
            User manager,
            String propertyIdStr,
            String status,
            String search,
            LocalDate checkInFrom,
            LocalDate checkInTo,
            int page,
            int size,
            String sort
    ) {
        Pageable pageable = (sort == null || sort.isBlank())
                ? PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "checkInDate"))
                : buildPageable(page, size, sort);

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

        Booking.Status bookingStatus = parseBookingStatus(status);
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<Booking> result = bookingRepository.findForManagerWithFilters(
                propertyIds, bookingStatus, cleanSearch, checkInFrom, checkInTo, pageable);

        return new PageResponse<>(
                result.getContent().stream().map(booking -> {
                    boolean isReviewed = reviewRepository.existsByBooking_Id(booking.getId());
                    return BookingSummaryResponse.fromEntity(booking, isReviewed);
                }).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private static Booking.Status parseBookingStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status.trim())) {
            return null;
        }
        try {
            return Booking.Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** SCR-35 — Manager booking detail (scoped). */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ManagerBookingDetailResponse getBookingDetailForManager(User manager, UUID id) {
        Booking booking = findBookingOrThrow(id);
        scopeValidator.validateManagerAccess(manager, booking.getRoom().getProperty().getId());
        return buildManagerDetail(booking);
    }

    /** SCR-37 — Upload CMND/CCCD for manager check-in flow. */
    @org.springframework.transaction.annotation.Transactional
    public List<String> uploadIdDocumentsForManager(User manager, UUID bookingId, List<MultipartFile> files) {
        Booking booking = findBookingOrThrow(bookingId);
        scopeValidator.validateManagerAccess(manager, booking.getRoom().getProperty().getId());

        if (files == null || files.isEmpty()) {
            throw new BusinessException("Cần ít nhất một ảnh giấy tờ");
        }
        if (files.size() > 3) {
            throw new BusinessException("Tối đa 3 ảnh giấy tờ");
        }

        Path uploadDir = Paths.get("uploads", "bookings", bookingId.toString()).toAbsolutePath();
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new BusinessException("Không thể tạo thư mục upload");
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            validateIdDocumentFile(file);
            String savedName = UUID.randomUUID() + "_" + sanitizeFilename(file.getOriginalFilename());
            Path target = uploadDir.resolve(savedName);
            try {
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new BusinessException("Không thể lưu ảnh giấy tờ");
            }
            urls.add("/uploads/bookings/" + bookingId + "/" + savedName);
        }

        if (urls.isEmpty()) {
            throw new BusinessException("Cần ít nhất một ảnh giấy tờ");
        }
        return urls;
    }

    /** SCR-37 — Manager check-in with verification (scoped). */
    @org.springframework.transaction.annotation.Transactional
    public ManagerBookingDetailResponse markAsCheckedInForManager(
            User manager, UUID id, ManagerCheckInRequest req) {
        Booking booking = findBookingOrThrow(id);
        scopeValidator.validateManagerAccess(manager, booking.getRoom().getProperty().getId());

        validateIdDocumentUrls(req.getIdDocumentUrls());
        if (!Boolean.TRUE.equals(req.getKeyHandedOver())) {
            throw new BusinessException("Phải xác nhận đã giao chìa khóa cho khách");
        }

        if (booking.getStatus() != Booking.Status.CONFIRMED) {
            throw new BusinessException("Chỉ có thể nhận phòng khi booking đã xác nhận");
        }
        if (booking.getCheckInDate().isAfter(LocalDate.now())) {
            throw new BusinessException("Chưa đến ngày nhận phòng");
        }

        List<Payment> payments = paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId());
        boolean remainingUnpaid = booking.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0
                && payments.stream().noneMatch(p ->
                p.getType() == Payment.Type.REMAINING_BALANCE && p.getStatus() == Payment.Status.PAID);
        if (remainingUnpaid && !Boolean.TRUE.equals(req.getRemainingCollected())) {
            throw new ConflictException("Chưa thu phần còn lại");
        }
        // Cash at desk (SCR-37): in-scope exception to TD-011 VNPay-only for online methods.
        if (remainingUnpaid && Boolean.TRUE.equals(req.getRemainingCollected())) {
            recordCashRemainingAtDesk(booking, manager);
        }

        booking.setStatus(Booking.Status.CHECKED_IN);
        booking.getRoom().setStatus(Room.Status.OCCUPIED);
        bookingRepository.save(booking);

        saveCheckVerification(booking, manager, BookingCheckVerification.Type.CHECK_IN, req);

        String roomName = booking.getRoom().getRoomNumber();
        notificationService.sendNotification(
                booking.getCustomer().getId(),
                com.homestay.entity.Notification.Type.BOOKING_CONFIRMED,
                "Đã nhận phòng",
                "Bạn đã check-in phòng " + roomName + ". Chúc bạn có kỳ nghỉ vui vẻ!",
                booking.getId(), "Booking"
        );

        return buildManagerDetail(booking);
    }

    /** SCR-37 — Manager check-out with inspection gate (scoped). */
    @org.springframework.transaction.annotation.Transactional
    public ManagerBookingDetailResponse markAsCheckedOutForManager(
            User manager, UUID id, ManagerCheckOutRequest req) {
        Booking booking = findBookingOrThrow(id);
        scopeValidator.validateManagerAccess(manager, booking.getRoom().getProperty().getId());

        if (!Boolean.TRUE.equals(req.getKeyReturned())) {
            throw new BusinessException("Phải xác nhận đã thu lại chìa khóa");
        }

        if (booking.getStatus() == Booking.Status.CHECKED_IN) {
            booking.setStatus(Booking.Status.PENDING_INSPECTION);
            ensureInspectionPending(booking);
            bookingRepository.save(booking);
            saveCheckOutVerification(booking, manager, req);
            return buildManagerDetail(booking);
        }

        if (booking.getStatus() == Booking.Status.PENDING_INSPECTION) {
            RoomInspection inspection = roomInspectionRepository.findByBookingId(id)
                    .orElseThrow(() -> new ConflictException("Chưa hoàn tất kiểm tra phòng"));
            assertCheckoutInspectionGate(id, inspection);
            booking.setStatus(Booking.Status.CHECKED_OUT);
            booking.getRoom().setStatus(Room.Status.PENDING_CLEANING);
            bookingRepository.save(booking);
            completeActiveContract(booking.getId());
            housekeepingTaskService.onBookingCheckedOut(booking);
            saveCheckOutVerification(booking, manager, req);

            String roomName = booking.getRoom().getRoomNumber();
            notificationService.sendNotification(
                    booking.getCustomer().getId(),
                    com.homestay.entity.Notification.Type.BOOKING_CONFIRMED,
                    "Đã trả phòng",
                    "Cảm ơn bạn đã lưu trú tại " + roomName + ". Hẹn gặp lại!",
                    booking.getId(), "Booking"
            );
            return buildManagerDetail(booking);
        }

        if (booking.getStatus() == Booking.Status.PENDING_DAMAGE_PAYMENT) {
            throw new ConflictException("Khách chưa thanh toán phí thiệt hại");
        }

        throw new BusinessException("Không thể trả phòng ở trạng thái hiện tại");
    }

    private void validateIdDocumentFile(MultipartFile file) {
        if (file.getSize() > MAX_ID_DOC_BYTES) {
            throw new BusinessException("Ảnh giấy tờ không được vượt quá 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null
                || (!contentType.equalsIgnoreCase("image/jpeg")
                && !contentType.equalsIgnoreCase("image/png"))) {
            throw new BusinessException("Chỉ chấp nhận ảnh JPG hoặc PNG");
        }
    }

    private static String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) {
            return "document.jpg";
        }
        return original.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void validateIdDocumentUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            throw new BusinessException("Cần ít nhất một ảnh giấy tờ");
        }
        if (urls.size() > 3) {
            throw new BusinessException("Tối đa 3 ảnh giấy tờ");
        }
        for (String url : urls) {
            if (url == null || !url.startsWith("/uploads/")) {
                throw new BusinessException("URL ảnh giấy tờ không hợp lệ");
            }
        }
    }

    /**
     * Records remaining balance collected as cash at reception during check-in.
     * Product decision 2026-07-23: cash-at-desk kept; TD-011 still defers bank transfer + CK verify only.
     */
    private void recordCashRemainingAtDesk(Booking booking, User manager) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setCustomer(booking.getCustomer());
        payment.setType(Payment.Type.REMAINING_BALANCE);
        payment.setMethod(Payment.Method.CASH);
        payment.setAmount(booking.getRemainingAmount());
        payment.setStatus(Payment.Status.PAID);
        payment.setPaidAt(java.time.LocalDateTime.now());
        payment.setVerifiedBy(manager);
        payment.setVerifiedAt(java.time.LocalDateTime.now());
        payment.setVerificationNote("Thu phần còn lại bằng tiền mặt tại quầy (check-in)");
        invoiceRepository.findByBooking_IdAndType(booking.getId(), Invoice.Type.REMAINING_BALANCE)
                .ifPresent(payment::setInvoice);
        paymentRepository.save(payment);
        invoiceStatusSyncService.syncFromPaymentEntity(payment);
    }

    private void saveCheckVerification(
            Booking booking,
            User manager,
            BookingCheckVerification.Type type,
            ManagerCheckInRequest req) {
        BookingCheckVerification record = new BookingCheckVerification();
        record.setBooking(booking);
        record.setType(type);
        record.setIdDocumentUrls(req.getIdDocumentUrls());
        record.setKeyHandedOver(req.getKeyHandedOver());
        record.setRemainingCollected(req.getRemainingCollected());
        record.setNote(req.getNote());
        record.setPerformedBy(manager);
        checkVerificationRepository.save(record);
    }

    private void saveCheckOutVerification(Booking booking, User manager, ManagerCheckOutRequest req) {
        BookingCheckVerification record = new BookingCheckVerification();
        record.setBooking(booking);
        record.setType(BookingCheckVerification.Type.CHECK_OUT);
        record.setKeyReturned(req.getKeyReturned());
        record.setNote(req.getNote());
        record.setPerformedBy(manager);
        checkVerificationRepository.save(record);
    }

    private Booking findBookingOrThrow(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new com.homestay.exception.ResourceNotFoundException("Booking không tồn tại"));
    }

    private ManagerBookingDetailResponse buildManagerDetail(Booking booking) {
        boolean isReviewed = reviewRepository.existsByBooking_Id(booking.getId());
        Optional<RoomInspection> inspection = roomInspectionRepository.findByBookingId(booking.getId());
        List<BookingDetailResponse.PaymentInfo> payments =
                paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()).stream()
                        .map(BookingDetailResponse.PaymentInfo::fromEntity)
                        .toList();
        ManagerBookingDetailResponse resp =
                ManagerBookingDetailResponse.fromBooking(booking, isReviewed, inspection, payments);
        resp.setInvoiceBreakdown(invoiceQueryService.buildBreakdown(booking));
        return resp;
    }

    private void ensureInspectionPending(Booking booking) {
        if (roomInspectionRepository.findByBookingId(booking.getId()).isPresent()) {
            return;
        }
        RoomInspection inspection = new RoomInspection();
        inspection.setBooking(booking);
        inspection.setRoom(booking.getRoom());
        inspection.setProperty(booking.getRoom().getProperty());
        inspection.setStatus(RoomInspection.Status.PENDING);
        roomInspectionRepository.save(inspection);
    }

    /**
     * FR-23 checkout gate (data-model.md): ALLOW when PASSED, or FAILED_WITH_DAMAGE
     * with damage report APPROVED/PAID and DAMAGE_FEE payment PAID.
     */
    private void assertCheckoutInspectionGate(UUID bookingId, RoomInspection inspection) {
        RoomInspection.Status status = inspection.getStatus();
        if (status == RoomInspection.Status.PASSED) {
            return;
        }
        if (status == RoomInspection.Status.PENDING || status == RoomInspection.Status.IN_PROGRESS) {
            throw new ConflictException("Chưa hoàn tất kiểm tra phòng");
        }
        if (status != RoomInspection.Status.FAILED_WITH_DAMAGE) {
            throw new ConflictException("Chưa hoàn tất kiểm tra phòng");
        }

        DamageReport report = damageReportRepository.findFirstByBooking_IdOrderByCreatedAtDesc(bookingId)
                .orElseThrow(() -> new ConflictException("Cần báo cáo hư hại trước khi trả phòng"));

        DamageReport.Status reportStatus = report.getStatus();
        if (reportStatus == DamageReport.Status.DRAFT
                || reportStatus == DamageReport.Status.PENDING_APPROVAL
                || reportStatus == DamageReport.Status.DISPUTED) {
            throw new ConflictException("Báo cáo hư hại chưa được phê duyệt hoặc đang tranh chấp");
        }
        if (reportStatus != DamageReport.Status.APPROVED && reportStatus != DamageReport.Status.PAID) {
            throw new ConflictException("Báo cáo hư hại chưa được phê duyệt hoặc đang tranh chấp");
        }

        boolean damageFeePaid = paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .anyMatch(p -> p.getType() == Payment.Type.DAMAGE_FEE
                        && p.getStatus() == Payment.Status.PAID);
        if (!damageFeePaid) {
            throw new ConflictException("Khách chưa thanh toán phí thiệt hại");
        }
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public BookingDetailResponse getBookingDetail(UUID id, User currentUser) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new com.homestay.exception.ResourceNotFoundException("Booking không tồn tại"));

        boolean isManager = currentUser.getRole() == User.Role.MANAGER;

        if (!isManager && !booking.getCustomer().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Không có quyền xem chi tiết đặt phòng này");
        }

        boolean isReviewed = reviewRepository.existsByBooking_Id(booking.getId());
        UUID reviewId = isReviewed
                ? reviewRepository.findIdByBooking_Id(booking.getId()).orElse(null)
                : null;
        BookingDetailResponse response = BookingDetailResponse.fromEntity(booking, isReviewed, reviewId);
        response.setPayments(
                paymentRepository.findByBookingIdOrderByCreatedAtDesc(id).stream()
                        .map(BookingDetailResponse.PaymentInfo::fromEntity)
                        .toList()
        );
        response.setInvoiceBreakdown(invoiceQueryService.buildBreakdown(booking));
        return response;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public CancellationPreviewResponse getCancellationPreview(UUID id, User currentUser) {
        if (currentUser.getRole() != User.Role.CUSTOMER) {
            throw new ForbiddenException("Chỉ khách hàng mới có thể xem chính sách hủy");
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new com.homestay.exception.ResourceNotFoundException("Booking không tồn tại"));

        if (!booking.getCustomer().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Không có quyền hủy booking này");
        }

        assertCancellableStatus(booking);
        int refundPercent = resolveCustomerCancelRefundPercent(booking);
        return buildCancellationPreview(booking, refundPercent, customerCancelPolicyText(refundPercent));
    }

    /** SCR-69 — Manager cancel preview (100% refund). */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public CancellationPreviewResponse getManagerCancellationPreview(User manager, UUID id) {
        Booking booking = findBookingOrThrow(id);
        scopeValidator.validateManagerAccess(manager, booking.getRoom().getProperty().getId());
        assertCancellableStatus(booking);
        return buildCancellationPreview(booking, MANAGER_CANCEL_REFUND_PERCENT,
                "Hủy bởi Manager (lỗi hệ thống/bất khả kháng): hoàn 100% tiền cọc đã thanh toán.");
    }

    @org.springframework.transaction.annotation.Transactional
    public void cancelBooking(UUID id, User currentUser) {
        cancelBooking(id, currentUser, null);
    }

    @org.springframework.transaction.annotation.Transactional
    public void cancelBooking(UUID id, User currentUser, String reason) {
        if (currentUser.getRole() == User.Role.MANAGER) {
            throw new ForbiddenException("Manager phải hủy qua API /api/v1/manager/bookings/{id}/cancel");
        }
        if (currentUser.getRole() != User.Role.CUSTOMER) {
            throw new ForbiddenException("Không có quyền hủy booking này");
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new com.homestay.exception.ResourceNotFoundException("Booking không tồn tại"));

        if (!booking.getCustomer().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Không có quyền hủy booking này");
        }

        assertCancellableStatus(booking);
        int refundPercent = resolveCustomerCancelRefundPercent(booking);
        applyCancellation(booking, currentUser, reason, refundPercent,
                "Booking cancelled by customer (" + refundPercent + "% deposit refund per FR-010).");
    }

    /** SCR-69 — Manager-initiated cancel with required reason and 100% refund. */
    @org.springframework.transaction.annotation.Transactional
    public ManagerBookingDetailResponse cancelBookingForManager(User manager, UUID id, String cancelReason) {
        if (cancelReason == null || cancelReason.isBlank()) {
            throw new BusinessException("Lý do hủy (cancelReason) là bắt buộc");
        }
        if (cancelReason.trim().length() > 500) {
            throw new BusinessException("Lý do hủy tối đa 500 ký tự");
        }

        Booking booking = findBookingOrThrow(id);
        scopeValidator.validateManagerAccess(manager, booking.getRoom().getProperty().getId());
        assertCancellableStatus(booking);

        applyCancellation(booking, manager, cancelReason.trim(), MANAGER_CANCEL_REFUND_PERCENT,
                "Booking cancelled by manager (100% refund of paid amount).");

        return buildManagerDetail(booking);
    }

    /**
     * FR-015 — Manager modify Confirmed booking (dates and/or room).
     * Recalculates total/deposit/remaining snapshots; returns priceDelta.
     */
    @org.springframework.transaction.annotation.Transactional
    public ModifyBookingResponse modifyBookingForManager(User manager, UUID id, ModifyBookingRequest request) {
        Booking booking = findBookingOrThrow(id);
        scopeValidator.validateManagerAccess(manager, booking.getRoom().getProperty().getId());

        if (booking.getStatus() != Booking.Status.CONFIRMED) {
            throw new ConflictException("Chỉ được sửa booking ở trạng thái CONFIRMED");
        }

        LocalDate newCheckIn = request.getCheckIn() != null ? request.getCheckIn() : booking.getCheckInDate();
        LocalDate newCheckOut = request.getCheckOut() != null ? request.getCheckOut() : booking.getCheckOutDate();
        UUID newRoomId = request.getRoomId() != null ? request.getRoomId() : booking.getRoom().getId();

        if (!newCheckOut.isAfter(newCheckIn)) {
            throw new IllegalArgumentException("Ngày check-out phải sau ngày check-in");
        }
        if (newCheckIn.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày check-in không được trong quá khứ");
        }

        Room newRoom = roomRepository.findByIdForUpdate(newRoomId)
                .orElseThrow(() -> new com.homestay.exception.ResourceNotFoundException("Phòng không tồn tại"));

        if (newRoom.getDeletedAt() != null) {
            throw new ConflictException("Phòng đã bị xóa và không thể đặt");
        }

        UUID propertyId = booking.getRoom().getProperty().getId();
        if (!newRoom.getProperty().getId().equals(propertyId)) {
            throw new BusinessException("Chỉ được đổi sang phòng cùng property");
        }
        scopeValidator.validateManagerAccess(manager, newRoom.getProperty().getId());

        if (BOOKING_BLOCKED_ROOM_STATUSES.contains(newRoom.getStatus())) {
            throw new ConflictException("Phòng mới không khả dụng (bảo trì / ngưng phục vụ / đang dọn)");
        }

        if (roomStatusBlockRepository.existsActiveOverlappingStay(newRoom.getId(), newCheckIn, newCheckOut)) {
            throw new ConflictException(
                    "Phòng đang bảo trì hoặc ngưng phục vụ trong khoảng ngày mới. Vui lòng chọn ngày khác.");
        }

        if (booking.getGuestCount() > newRoom.getCapacity()) {
            throw new IllegalArgumentException("Số khách vượt sức chứa phòng mới");
        }

        boolean overlap = roomRepository.existsOverlapBookingExcluding(
                newRoom.getId(), newCheckIn, newCheckOut, booking.getId());
        if (overlap) {
            throw new ConflictException("Phòng đã có booking trùng khoảng ngày mới");
        }

        BigDecimal oldTotal = booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO;
        long nights = java.time.temporal.ChronoUnit.DAYS.between(newCheckIn, newCheckOut);
        if (nights <= 0) nights = 1;
        BigDecimal newTotal = newRoom.getPricePerNight().multiply(BigDecimal.valueOf(nights));
        BigDecimal newDeposit = newTotal.multiply(adminSettingsService.getDepositPercentage())
                .setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal newRemaining = newTotal.subtract(newDeposit);
        BigDecimal priceDelta = newTotal.subtract(oldTotal);

        booking.setRoom(newRoom);
        booking.setCheckInDate(newCheckIn);
        booking.setCheckOutDate(newCheckOut);
        booking.setTotalAmount(newTotal);
        booking.setDepositAmount(newDeposit);
        booking.setRemainingAmount(newRemaining);
        bookingRepository.save(booking);
        invoiceAdjustmentService.adjustOnBookingModify(booking);

        notificationService.sendNotification(
                booking.getCustomer().getId(),
                com.homestay.entity.Notification.Type.BOOKING_CONFIRMED,
                "Booking Updated",
                "Your booking #" + booking.getId().toString().substring(0, 8).toUpperCase()
                        + " was updated. New total: " + newTotal
                        + (priceDelta.signum() == 0 ? "." : ". Price change: " + priceDelta + "."),
                booking.getId(), "Booking"
        );

        String message = priceDelta.signum() > 0
                ? "Đã cập nhật booking. Khách cần thanh toán thêm " + priceDelta + " ₫."
                : priceDelta.signum() < 0
                ? "Đã cập nhật booking. Chênh lệch giảm " + priceDelta.abs() + " ₫ trên phần còn lại."
                : "Đã cập nhật booking. Tổng tiền không đổi.";

        return new ModifyBookingResponse(buildManagerDetail(booking), priceDelta, message);
    }

    private CancellationPreviewResponse buildCancellationPreview(Booking booking, int refundPercent, String policyText) {
        long daysUntilCheckIn = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.now(), booking.getCheckInDate());
        if (daysUntilCheckIn < 0) {
            daysUntilCheckIn = 0;
        }

        // FR-010: tiers apply to paid deposit (cọc), not remaining/other payments
        BigDecimal paidDeposit = sumPaidDepositAmount(booking.getId());
        BigDecimal refundAmount = paidDeposit
                .multiply(BigDecimal.valueOf(refundPercent))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal forfeitAmount = paidDeposit.subtract(refundAmount);

        return new CancellationPreviewResponse(
                (int) daysUntilCheckIn,
                refundPercent,
                refundAmount,
                forfeitAmount,
                policyText
        );
    }

    /**
     * FR-010 customer cancel tiers (calendar days until check-in):
     * ≥7 → 100%; 3–6 → 50%; &lt;3 → 0%. Day 7+ uses 100% (≥7). PENDING_DEPOSIT unpaid → 0%.
     */
    static int resolveCustomerCancelRefundPercent(Booking booking) {
        if (booking.getStatus() == Booking.Status.PENDING_DEPOSIT) {
            return 0;
        }
        long daysUntilCheckIn = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.now(), booking.getCheckInDate());
        if (daysUntilCheckIn >= 7) {
            return 100;
        }
        if (daysUntilCheckIn >= 3) {
            return 50;
        }
        return 0;
    }

    private static String customerCancelPolicyText(int refundPercent) {
        return switch (refundPercent) {
            case 100 -> "Hủy bởi khách (≥7 ngày trước check-in): hoàn 100% tiền cọc đã thanh toán.";
            case 50 -> "Hủy bởi khách (3 đến dưới 7 ngày trước check-in): hoàn 50% tiền cọc đã thanh toán.";
            default -> "Hủy bởi khách (<3 ngày trước check-in): không hoàn tiền cọc.";
        };
    }

    private BigDecimal sumPaidDepositAmount(UUID bookingId) {
        return paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .filter(p -> p.getType() == Payment.Type.DEPOSIT && p.getStatus() == Payment.Status.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void assertCancellableStatus(Booking booking) {
        Booking.Status status = booking.getStatus();
        if (status == Booking.Status.CANCELLED) {
            throw new IllegalArgumentException("Booking đã bị hủy trước đó");
        }
        if (status == Booking.Status.CHECKED_IN
                || status == Booking.Status.CHECKED_OUT
                || status == Booking.Status.PENDING_INSPECTION
                || status == Booking.Status.PENDING_DAMAGE_PAYMENT
                || status == Booking.Status.NO_SHOW) {
            throw new IllegalArgumentException("Không thể hủy booking sau khi check-in đã bắt đầu hoặc ở trạng thái hiện tại");
        }
        if (status != Booking.Status.PENDING_DEPOSIT && status != Booking.Status.CONFIRMED) {
            throw new IllegalArgumentException("Booking không thể hủy ở trạng thái hiện tại");
        }
    }

    private void applyCancellation(Booking booking, User actor, String reason, int refundPercent, String notifyDetail) {
        CancellationPreviewResponse preview = buildCancellationPreview(booking, refundPercent, notifyDetail);

        booking.setStatus(Booking.Status.CANCELLED);
        booking.setCancelledAt(java.time.LocalDateTime.now());
        booking.setCancelledBy(actor);
        if (reason != null && !reason.isBlank()) {
            booking.setCancelReason(reason.trim());
        }
        bookingRepository.save(booking);

        applyPaymentRefunds(booking, refundPercent);
        invoiceCancellationService.cancelUnpaidForBooking(booking.getId());
        cancelActiveContract(booking.getId());
        releaseStaleRoomHoldStatus(booking.getRoom());

        String shortId = booking.getId().toString().substring(0, 8).toUpperCase();
        notificationService.sendNotification(
                booking.getCustomer().getId(),
                com.homestay.entity.Notification.Type.BOOKING_CANCELLED,
                "Booking Cancelled",
                "Your booking #" + shortId + " has been cancelled. Refund: "
                        + preview.getRefundPercent() + "% (" + preview.getRefundAmount() + " VND).",
                booking.getId(), "Booking"
        );
    }

    private void applyPaymentRefunds(Booking booking, int refundPercent) {
        List<Payment> payments = paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId());
        for (Payment payment : payments) {
            if (payment.getStatus() == Payment.Status.PENDING) {
                payment.setStatus(Payment.Status.FAILED);
                payment.setVerificationNote("Cancelled with booking — pending payment voided");
                continue;
            }
            if (payment.getStatus() != Payment.Status.PAID) {
                continue;
            }
            // FR-010: refund policy applies to deposit; remaining paid stays as-is for v1 (no auto VNPay payout)
            if (payment.getType() == Payment.Type.DEPOSIT) {
                applyDepositCancelRefund(payment, refundPercent);
            } else {
                payment.setVerificationNote("Booking cancelled — payment type " + payment.getType()
                        + " not auto-refunded by customer cancel policy");
            }
        }
        if (!payments.isEmpty()) {
            paymentRepository.saveAll(payments);
        }
    }

    /**
     * Auditable cancel refund on deposit: keep {@code amount} as original paid deposit;
     * encode refund/forfeit in status + verificationNote (manual VNPay payout OK for v1).
     * 100% → REFUNDED; 50% → PARTIALLY_REFUNDED; 0% → stays PAID (forfeit).
     */
    private static void applyDepositCancelRefund(Payment payment, int refundPercent) {
        BigDecimal depositAmount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
        BigDecimal refundAmount = depositAmount
                .multiply(BigDecimal.valueOf(refundPercent))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal forfeitAmount = depositAmount.subtract(refundAmount);

        String audit = "FR-010 cancel: refundPercent=" + refundPercent
                + "; refundAmount=" + refundAmount.toPlainString()
                + "; depositAmount=" + depositAmount.toPlainString()
                + "; forfeitAmount=" + forfeitAmount.toPlainString()
                + "; payout=manual";

        if (refundPercent <= 0) {
            payment.setVerificationNote("Cancel: deposit forfeited (0% refund per FR-010). " + audit);
            return;
        }
        if (refundPercent >= 100) {
            payment.setStatus(Payment.Status.REFUNDED);
            payment.setVerificationNote("Cancel refund 100% of deposit (FR-010; payout may be manual). " + audit);
            return;
        }
        payment.setStatus(Payment.Status.PARTIALLY_REFUNDED);
        payment.setVerificationNote("Cancel partial refund " + refundPercent
                + "% of deposit (FR-010; payout may be manual). " + audit);
    }

    private void cancelActiveContract(UUID bookingId) {
        contractRepository.findByBookingId(bookingId).ifPresent(contract -> {
            if (contract.getStatus() == Contract.Status.ACTIVE) {
                contract.setStatus(Contract.Status.CANCELLED);
                contractRepository.save(contract);
            }
        });
    }

    /** FR-011 — Checked-out booking → contract Completed. */
    private void completeActiveContract(UUID bookingId) {
        contractRepository.findByBookingId(bookingId).ifPresent(contract -> {
            if (contract.getStatus() == Contract.Status.ACTIVE) {
                contract.setStatus(Contract.Status.COMPLETED);
                contractRepository.save(contract);
            }
        });
    }

    @org.springframework.transaction.annotation.Transactional
    public void markAsCheckedIn(UUID id) {
        throw new com.homestay.exception.BusinessException(
                "Legacy markAsCheckedIn đã bị vô hiệu. Dùng ManagerBookingV1Service.checkIn (CMND + remaining).");
    }

    @org.springframework.transaction.annotation.Transactional
    public void markAsCheckedOut(UUID id) {
        throw new com.homestay.exception.BusinessException(
                "Legacy markAsCheckedOut đã bị vô hiệu. Dùng ManagerBookingV1Service.checkOut (inspection gate).");
    }

    @org.springframework.transaction.annotation.Transactional
    public BookingDetailResponse createBooking(com.homestay.dto.request.CreateBookingRequest request, User currentUser) {
        if (currentUser.getRole() != User.Role.CUSTOMER) {
            throw new ForbiddenException("Chỉ khách hàng mới có thể đặt phòng");
        }

        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new IllegalArgumentException("Ngày check-out phải sau ngày check-in");
        }

        if (request.getCheckInDate().isBefore(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Ngày check-in không được trong quá khứ");
        }

        Room room = roomRepository.findByIdForUpdate(request.getRoomId())
                .orElseThrow(() -> new com.homestay.exception.ResourceNotFoundException("Phòng không tồn tại"));

        if (room.getDeletedAt() != null) {
            throw new ConflictException("Phòng đã bị xóa và không thể đặt");
        }

        if (BOOKING_BLOCKED_ROOM_STATUSES.contains(room.getStatus())) {
            throw new ConflictException("Phòng hiện không khả dụng để đặt (bảo trì / ngưng phục vụ / đang dọn)");
        }

        if (roomStatusBlockRepository.existsActiveOverlappingStay(
                room.getId(), request.getCheckInDate(), request.getCheckOutDate())) {
            throw new ConflictException(
                    "Phòng đang bảo trì hoặc ngưng phục vụ trong khoảng ngày đã chọn. Vui lòng chọn ngày khác.");
        }

        if (request.getGuestCount() > room.getCapacity()) {
            throw new IllegalArgumentException("Số người vượt quá sức chứa của phòng");
        }

        boolean isOverlap = roomRepository.existsOverlapBooking(
                room.getId(), request.getCheckInDate(), request.getCheckOutDate());
        if (isOverlap) {
            throw new ConflictException(
                    "Phòng đang được giữ hoặc đã đặt trong khoảng ngày này. Vui lòng chọn ngày khác hoặc thử lại sau khi hết thời gian giữ chỗ.");
        }

        long nights = java.time.temporal.ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (nights <= 0) nights = 1;

        java.math.BigDecimal totalAmount = room.getPricePerNight().multiply(java.math.BigDecimal.valueOf(nights));
        java.math.BigDecimal depositAmount = totalAmount.multiply(adminSettingsService.getDepositPercentage())
                .setScale(2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal remainingAmount = totalAmount.subtract(depositAmount);

        Booking booking = new Booking();
        booking.setCustomer(currentUser);
        booking.setRoom(room);
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setGuestCount(request.getGuestCount());
        booking.setSpecialRequests(request.getSpecialRequests());
        booking.setTotalAmount(totalAmount);
        booking.setDepositAmount(depositAmount);
        booking.setRemainingAmount(remainingAmount);
        booking.setStatus(Booking.Status.PENDING_DEPOSIT);
        booking.setHoldExpiresAt(java.time.LocalDateTime.now().plusMinutes(holdTimeoutMinutes));

        booking = bookingRepository.save(booking);
        // Do NOT set Room.status = PENDING_DEPOSIT — inventory is date-range based (Spec FR-04).
        // FR-11: issue deposit + remaining invoice pair (idempotent).
        invoiceIssuanceService.issuePair(booking);

        return BookingDetailResponse.fromEntity(booking);
    }

    /**
     * Hold timeout: cancel unpaid PENDING_DEPOSIT past holdExpiresAt and release inventory.
     * @return number of bookings cancelled
     */
    @org.springframework.transaction.annotation.Transactional
    public int cancelExpiredDepositHolds() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<Booking> expired = bookingRepository.findExpiredPendingDeposits(now);
        int cancelled = 0;
        for (Booking booking : expired) {
            boolean depositPaid = paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()).stream()
                    .anyMatch(p -> p.getType() == Payment.Type.DEPOSIT && p.getStatus() == Payment.Status.PAID);
            if (depositPaid) {
                continue;
            }
            booking.setStatus(Booking.Status.CANCELLED);
            booking.setCancelledAt(now);
            booking.setCancelReason("Hold timeout — unpaid deposit");
            bookingRepository.save(booking);
            invoiceCancellationService.cancelUnpaidForBooking(booking.getId());
            releaseStaleRoomHoldStatus(booking.getRoom());
            cancelled++;

            notificationService.sendNotification(
                    booking.getCustomer().getId(),
                    com.homestay.entity.Notification.Type.BOOKING_CANCELLED,
                    "Booking Cancelled",
                    "Your booking #" + booking.getId().toString().substring(0, 8).toUpperCase()
                            + " was cancelled because the deposit was not paid in time.",
                    booking.getId(), "Booking"
            );
        }
        return cancelled;
    }

    /**
     * FR-008: CONFIRMED → NO_SHOW when more than {@code noShowAfterHours} (default 24)
     * past {@code checkInDate.atStartOfDay()}. Deposit kept (payments unchanged).
     *
     * @return number of bookings marked NO_SHOW
     */
    @org.springframework.transaction.annotation.Transactional
    public int markNoShowExpiredConfirmed() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        // Coarse filter: check-ins that could possibly be past the hour threshold
        LocalDate maxCheckInDate = now.toLocalDate();
        List<Booking> candidates = bookingRepository.findConfirmedEligibleForNoShow(maxCheckInDate);
        int marked = 0;
        for (Booking booking : candidates) {
            if (booking.getStatus() != Booking.Status.CONFIRMED) {
                continue;
            }
            if (booking.getCheckInDate() == null) {
                continue;
            }
            java.time.LocalDateTime deadline = booking.getCheckInDate().atStartOfDay().plusHours(noShowAfterHours);
            if (!now.isAfter(deadline)) {
                continue;
            }
            booking.setStatus(Booking.Status.NO_SHOW);
            bookingRepository.save(booking);
            releaseStaleRoomHoldStatus(booking.getRoom());
            cancelActiveContract(booking.getId());
            marked++;

            notificationService.sendNotification(
                    booking.getCustomer().getId(),
                    com.homestay.entity.Notification.Type.BOOKING_CANCELLED,
                    "Booking No-Show",
                    "Your booking #" + booking.getId().toString().substring(0, 8).toUpperCase()
                            + " was marked as no-show because check-in did not occur within 24 hours of the expected check-in date. The deposit is not refunded.",
                    booking.getId(), "Booking"
            );
        }
        return marked;
    }

    /** Clear legacy PENDING_DEPOSIT/RESERVED room flags when no blocking bookings remain. */
    private void releaseStaleRoomHoldStatus(Room room) {
        if (room == null) {
            return;
        }
        if (room.getStatus() != Room.Status.PENDING_DEPOSIT && room.getStatus() != Room.Status.RESERVED) {
            return;
        }
        if (!roomRepository.hasBlockingBookings(room.getId())) {
            room.setStatus(Room.Status.AVAILABLE);
            roomRepository.save(room);
        }
    }
}
