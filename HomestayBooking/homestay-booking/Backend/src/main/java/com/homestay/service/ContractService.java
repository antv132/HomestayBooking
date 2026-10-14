package com.homestay.service;

import com.homestay.dto.response.ContractDetailResponse;
import com.homestay.dto.response.ContractSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.Contract;
import com.homestay.entity.Property;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ForbiddenException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.ContractRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.util.UploadLimits;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final com.homestay.repository.BookingRepository bookingRepository;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final ManagerPropertyAssignmentRepository assignmentRepository;
    private final ReportPropertyScopeValidator scopeValidator;
    private final OutboxPublisher outboxPublisher;
    private final ActivityLogService activityLogService;
    private final String uploadDir;

    public ContractService(ContractRepository contractRepository,
                           com.homestay.repository.BookingRepository bookingRepository,
                           PdfService pdfService, EmailService emailService,
                           NotificationService notificationService,
                           ManagerPropertyAssignmentRepository assignmentRepository,
                           ReportPropertyScopeValidator scopeValidator,
                           OutboxPublisher outboxPublisher,
                           ActivityLogService activityLogService,
                           @Value("${app.upload.dir:uploads/}") String uploadDir) {
        this.contractRepository = contractRepository;
        this.bookingRepository = bookingRepository;
        this.pdfService = pdfService;
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.assignmentRepository = assignmentRepository;
        this.scopeValidator = scopeValidator;
        this.outboxPublisher = outboxPublisher;
        this.activityLogService = activityLogService;
        this.uploadDir = uploadDir;
    }

    @Transactional(readOnly = true)
    public PageResponse<ContractSummaryResponse> getAllContracts(int page, int size, String status, String search, String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        Contract.Status contractStatus = parseStatus(status);
        String searchParam = cleanSearch(search);

        Page<Contract> result = contractRepository.findAllWithFilters(contractStatus, searchParam, pageable);

        return toSummaryPage(result);
    }

    /** SCR-38 — Manager list scoped to assigned properties (optional propertyId filter). */
    @Transactional(readOnly = true)
    public PageResponse<ContractSummaryResponse> getManagerContracts(
            User manager,
            String propertyIdStr,
            int page,
            int size,
            String status,
            String search,
            String sort
    ) {
        Pageable pageable = buildPageable(page, size, sort);
        Contract.Status contractStatus = parseStatus(status);
        String searchParam = cleanSearch(search);

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

        Page<Contract> result = contractRepository.findByPropertyIdsWithFilters(
                propertyIds, contractStatus, searchParam, pageable);
        return toSummaryPage(result);
    }

    @Transactional(readOnly = true)
    public PageResponse<ContractSummaryResponse> getMyContracts(User currentUser, int page, int size, String status, String search, String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        Contract.Status contractStatus = parseStatus(status);
        String searchParam = cleanSearch(search);

        Page<Contract> result = contractRepository.findByCustomerWithFilters(
                currentUser.getId(), contractStatus, searchParam, pageable);

        return toSummaryPage(result);
    }

    @Transactional(readOnly = true)
    public ContractDetailResponse getContractDetail(UUID id, User currentUser) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract không tồn tại"));
        assertCanViewContract(contract, currentUser);
        return ContractDetailResponse.fromEntity(contract);
    }

    /** Read-only lookup by booking — does not create (FR-10: generate only via Outbox). */
    @Transactional(readOnly = true)
    public ContractDetailResponse getContractByBookingId(UUID bookingId, User currentUser) {
        com.homestay.entity.Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking không tồn tại"));

        if (currentUser.getRole() == User.Role.MANAGER) {
            scopeValidator.validateManagerAccess(currentUser, booking.getRoom().getProperty().getId());
        } else if (currentUser.getRole() != User.Role.ADMIN
                && !booking.getCustomer().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Không có quyền xem hợp đồng này");
        }

        Contract contract = contractRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chưa có hợp đồng cho booking này. Hợp đồng được tạo tự động sau khi cọc thành công."));
        return ContractDetailResponse.fromEntity(contract);
    }

    /**
     * Download contract PDF. If file chưa được tạo (outbox fail / chưa kịp generate),
     * tạo snapshot một lần rồi trả về — vẫn giữ immutability sau khi đã có file.
     */
    @Transactional
    public byte[] downloadContractPdf(UUID id, User currentUser) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract không tồn tại"));
        assertCanViewContract(contract, currentUser);
        return ensureContractPdfBytes(contract, currentUser);
    }

    /**
     * FR-009 — Manager resend: enqueue Outbox CONTRACT_RESEND (no sync send, no PDF regenerate).
     */
    @Transactional
    public void resendContractEmail(UUID id, User currentUser, String targetEmail) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract không tồn tại"));

        if (currentUser.getRole() != User.Role.MANAGER) {
            throw new ForbiddenException("Chỉ Manager được gửi lại email hợp đồng");
        }
        scopeValidator.validateManagerAccess(currentUser, contract.getRoom().getProperty().getId());

        outboxPublisher.enqueueContractResend(id, targetEmail, currentUser.getId());
    }

    /** @deprecated Prefer {@link #resendContractEmail(UUID, User, String)} with RBAC + Outbox. */
    @Transactional
    public void resendContractEmail(UUID id, String targetEmail) {
        processContractResendFromOutbox(id, targetEmail, null);
    }

    private void assertCanViewContract(Contract contract, User currentUser) {
        if (currentUser.getRole() == User.Role.MANAGER) {
            scopeValidator.validateManagerAccess(currentUser, contract.getRoom().getProperty().getId());
            return;
        }
        if (currentUser.getRole() == User.Role.ADMIN) {
            return;
        }
        if (!contract.getCustomer().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Không có quyền xem hợp đồng này");
        }
    }

    private PageResponse<ContractSummaryResponse> toSummaryPage(Page<Contract> result) {
        return new PageResponse<>(
                result.getContent().stream().map(ContractSummaryResponse::fromEntity).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private String cleanSearch(String search) {
        return (search != null && !search.isBlank()) ? search.trim() : null;
    }

    private Pageable buildPageable(int page, int size, String sort) {
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "generatedAt"));
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        // Map API-friendly aliases to entity fields
        if ("createdAt".equals(field)) {
            field = "generatedAt";
        }
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(direction, field));
    }

    private Contract.Status parseStatus(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) return null;
        try {
            return Contract.Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Transactional
    public void autoGenerateAndSendContract(UUID bookingId, User currentUser) {
        // FR-10 / TD-006: enqueue outbox in same TX as deposit confirm — worker generates PDF + email
        UUID actorId = currentUser != null ? currentUser.getId() : null;
        outboxPublisher.enqueueContractGenerate(bookingId, actorId);
    }

    /**
     * Outbox worker — create contract + PDF idempotently, then enqueue CONTRACT_EMAIL_SEND.
     */
    @Transactional
    public void processContractGenerateFromOutbox(UUID bookingId, User actor) {
        com.homestay.entity.Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking không tồn tại"));

        if (booking.getStatus() == com.homestay.entity.Booking.Status.PENDING_DEPOSIT
                || booking.getStatus() == com.homestay.entity.Booking.Status.CANCELLED
                || booking.getStatus() == com.homestay.entity.Booking.Status.NO_SHOW) {
            throw new IllegalArgumentException("Booking chưa đủ điều kiện sinh hợp đồng");
        }

        Contract contract = contractRepository.findByBookingId(bookingId)
                .orElseGet(() -> {
                    Contract newContract = new Contract();
                    newContract.setBooking(booking);
                    newContract.setCustomer(booking.getCustomer());
                    newContract.setRoom(booking.getRoom());
                    newContract.setDepositAmount(booking.getDepositAmount() != null
                            ? booking.getDepositAmount() : java.math.BigDecimal.ZERO);
                    newContract.setTotalAmount(booking.getTotalAmount() != null
                            ? booking.getTotalAmount() : java.math.BigDecimal.ZERO);
                    newContract.setCheckInDate(booking.getCheckInDate());
                    newContract.setCheckOutDate(booking.getCheckOutDate());
                    newContract.setStatus(Contract.Status.ACTIVE);
                    LocalDateTime now = LocalDateTime.now();
                    newContract.setGeneratedAt(now);
                    newContract.setCreatedAt(now);
                    Contract saved = contractRepository.save(newContract);

                    notificationService.sendNotification(
                            booking.getCustomer().getId(),
                            com.homestay.entity.Notification.Type.CONTRACT_GENERATED,
                            "Contract Generated",
                            "Your accommodation contract for booking has been generated and will be emailed shortly.",
                            saved.getId(), "Contract"
                    );
                    return saved;
                });

        if (contract.getPdfUrl() == null || contract.getPdfUrl().isBlank()) {
            persistContractPdf(contract, actor);
        }

        UUID actorId = actor != null ? actor.getId() : null;
        outboxPublisher.enqueueContractEmailSend(contract.getId(), actorId);
    }

    /** Outbox — first email after generate; ActivityLog CONTRACT_EMAIL_SENT. */
    @Transactional
    public void processContractEmailSendFromOutbox(UUID contractId, String targetEmail, User actor) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract không tồn tại"));
        sendContractEmailInternal(contract, targetEmail);
        activityLogService.log(actor, ActivityLogService.CONTRACT_EMAIL_SENT, "Contract",
                contract.getId(), "Contract email sent to "
                        + (targetEmail != null && !targetEmail.isBlank()
                        ? targetEmail
                        : contract.getCustomer().getEmail()));
    }

    /** Outbox worker — resend existing PDF snapshot; update sentAt; ActivityLog CONTRACT_RESENT. */
    @Transactional
    public void processContractResendFromOutbox(UUID contractId, String targetEmail, User actor) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract không tồn tại"));
        sendContractEmailInternal(contract, targetEmail);
        activityLogService.log(actor, ActivityLogService.CONTRACT_RESENT, "Contract",
                contract.getId(), "Contract email resent to "
                        + (targetEmail != null && !targetEmail.isBlank()
                        ? targetEmail
                        : contract.getCustomer().getEmail()));
    }

    private void sendContractEmailInternal(Contract contract, String targetEmail) {
        byte[] pdfBytes = loadStoredPdfStrict(contract);

        String email = targetEmail != null && !targetEmail.isBlank()
                ? targetEmail
                : contract.getCustomer().getEmail();
        String subject = "Accommodation Contract - Booking #" + contract.getBooking().getId();
        String text = "Dear " + (contract.getCustomer().getFullName() != null
                ? contract.getCustomer().getFullName() : "Customer") + ",\n\n" +
                "Please find attached your accommodation contract for your upcoming stay at " +
                contract.getRoom().getProperty().getName() + ".\n\n" +
                "Thank you for choosing us!\n";

        emailService.sendEmailWithAttachment(email, subject, text, pdfBytes, "Contract_" + contract.getId() + ".pdf");

        contract.setSentAt(LocalDateTime.now());
        contractRepository.save(contract);
    }

    /**
     * Generate PDF once, store under {@code /uploads/contracts/}, persist {@code pdfUrl}.
     * Uses existing {@code app.upload.dir} convention (spec assumption: /uploads/contracts/).
     */
    private void persistContractPdf(Contract contract, User actor) {
        byte[] pdfBytes = pdfService.generateContractPdf(contract);
        if (pdfBytes.length > UploadLimits.MAX_BYTES) {
            throw new BusinessException("File hợp đồng PDF vượt quá giới hạn 10MB");
        }

        Path contractsDir = Paths.get(uploadDir, "contracts");
        try {
            Files.createDirectories(contractsDir);
        } catch (IOException e) {
            throw new BusinessException("Không thể tạo thư mục upload contracts");
        }

        String fileName = contract.getId() + ".pdf";
        Path filePath = contractsDir.resolve(fileName);
        try {
            Files.write(filePath, pdfBytes);
        } catch (IOException e) {
            throw new BusinessException("Lưu file PDF hợp đồng thất bại");
        }

        contract.setPdfUrl("/uploads/contracts/" + fileName);
        if (contract.getGeneratedAt() == null) {
            contract.setGeneratedAt(LocalDateTime.now());
        }
        contractRepository.save(contract);

        activityLogService.log(actor, ActivityLogService.CONTRACT_GENERATED, "Contract",
                contract.getId(), "Contract PDF stored at " + contract.getPdfUrl());
    }

    /** Immutable snapshot — never regenerate PDF if file missing. */
    private byte[] loadStoredPdfStrict(Contract contract) {
        String pdfUrl = contract.getPdfUrl();
        if (pdfUrl == null || pdfUrl.isBlank() || !pdfUrl.startsWith("/uploads/")) {
            throw new BusinessException("Hợp đồng chưa có file PDF. Vui lòng đợi hệ thống tạo xong.");
        }
        Path filePath = Paths.get(uploadDir).resolve(pdfUrl.replaceFirst("^/uploads/", ""));
        if (!Files.isRegularFile(filePath)) {
            throw new BusinessException(
                    "File PDF hợp đồng không tìm thấy trên máy chủ. Liên hệ hỗ trợ — không tự tạo lại snapshot.");
        }
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new BusinessException("Không đọc được file PDF hợp đồng");
        }
    }

    /**
     * For customer/manager view: use stored file if present; otherwise generate once.
     * Email/resend still uses {@link #loadStoredPdfStrict} (no silent rewrite of sent snapshot).
     */
    private byte[] ensureContractPdfBytes(Contract contract, User actor) {
        String pdfUrl = contract.getPdfUrl();
        if (pdfUrl != null && !pdfUrl.isBlank() && pdfUrl.startsWith("/uploads/")) {
            Path filePath = Paths.get(uploadDir).resolve(pdfUrl.replaceFirst("^/uploads/", ""));
            if (Files.isRegularFile(filePath)) {
                try {
                    return Files.readAllBytes(filePath);
                } catch (IOException e) {
                    throw new BusinessException("Không đọc được file PDF hợp đồng");
                }
            }
        }
        persistContractPdf(contract, actor);
        return loadStoredPdfStrict(contract);
    }
}
