package com.homestay.service;

import com.homestay.dto.request.ApproveDamageReportRequest;
import com.homestay.dto.request.RejectDamageReportRequest;
import com.homestay.dto.response.DamageReportDetailResponse;
import com.homestay.dto.response.DamageReportSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.Attachment;
import com.homestay.entity.DamageItem;
import com.homestay.entity.DamageReport;
import com.homestay.entity.Notification;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.AttachmentRepository;
import com.homestay.repository.DamageReportRepository;
import com.homestay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * SCR-43 — Damage Report Management (Manager only).
 * Duyệt / từ chối / escalate báo cáo hư hại; scope theo property qua inspection.property.
 * KHÔNG đụng SCR-64/63 (Employee), SCR-53 (Admin co-approve), luồng Customer.
 */
@Service
@RequiredArgsConstructor
public class DamageReportManagerService {

    private static final String ATTACHMENT_ENTITY_TYPE = "DamageItem";

    private final DamageReportRepository damageReportRepository;
    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final ReportPropertyScopeValidator scopeValidator;
    private final NotificationService notificationService;
    private final DamageFeeSettlementService damageFeeSettlementService;

    @Transactional(readOnly = true)
    public PageResponse<DamageReportSummaryResponse> listForManager(
            User manager, UUID propertyId, DamageReport.Status status, Boolean escalated,
            String search, int page, int size) {

        scopeValidator.validateManagerAccess(manager, propertyId);

        Pageable pageable = PageRequest.of(page, size);
        Page<DamageReport> result =
                damageReportRepository.findForManagerBoard(propertyId, status, escalated, search, pageable);

        List<DamageReportSummaryResponse> content = result.getContent().stream()
                .map(DamageReportSummaryResponse::fromEntity)
                .toList();

        return new PageResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public DamageReportDetailResponse getDetailForManager(User manager, UUID id) {
        DamageReport dr = damageReportRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo hư hại"));
        scopeValidator.validateManagerAccess(manager, dr.getInspection().getProperty().getId());
        return toDetail(dr);
    }

    @Transactional
    public DamageReportDetailResponse approveForManager(User manager, UUID id, ApproveDamageReportRequest req) {
        DamageReport dr = loadPendingForManager(manager, id);

        if (Boolean.TRUE.equals(dr.getRequiresAdminEscalation())) {
            if (dr.getApprovedBy() != null) {
                throw new BusinessException("Báo cáo đã được chuyển Admin duyệt");
            }
            // Fee > 5M: Manager xác nhận rồi chuyển Admin đồng phê duyệt (SCR-53). Giữ PENDING_APPROVAL.
            dr.setApprovedBy(manager);
            dr.setApprovedAt(LocalDateTime.now());
            if (req.getNote() != null && !req.getNote().isBlank()) {
                dr.setNote(req.getNote().trim());
            }
            DamageReport saved = damageReportRepository.save(dr);
            notifyAdmins(saved);
            return toDetail(saved);
        }

        dr.setStatus(DamageReport.Status.APPROVED);
        dr.setApprovedBy(manager);
        dr.setApprovedAt(LocalDateTime.now());
        dr.setApprovedAmount(req.getApprovedAmount() != null ? req.getApprovedAmount() : dr.getTotalEstimatedCost());
        if (req.getNote() != null && !req.getNote().isBlank()) {
            dr.setNote(req.getNote().trim());
        }

        damageFeeSettlementService.applyApprovedFee(dr, dr.getApprovedAmount());

        DamageReport saved = damageReportRepository.save(dr);
        notifyCustomer(saved, "Báo cáo hư hại của bạn đã được duyệt bồi thường.");
        return toDetail(saved);
    }

    @Transactional
    public DamageReportDetailResponse rejectForManager(User manager, UUID id, RejectDamageReportRequest req) {
        DamageReport dr = loadPendingForManager(manager, id);
        if (Boolean.TRUE.equals(dr.getRequiresAdminEscalation()) && dr.getApprovedBy() != null) {
            throw new BusinessException("Báo cáo đã chuyển Admin — không thể từ chối tại Manager");
        }

        dr.setStatus(DamageReport.Status.DRAFT);
        dr.setNote(req.getReason().trim());

        DamageReport saved = damageReportRepository.save(dr);

        User inspector = saved.getInspection().getInspectedBy();
        if (inspector != null) {
            notificationService.sendNotification(
                    inspector.getId(),
                    Notification.Type.SYSTEM,
                    "Báo cáo hư hại bị trả lại",
                    "Báo cáo hư hại bị trả lại: " + req.getReason().trim(),
                    saved.getId(),
                    "DamageReport");
        }
        return toDetail(saved);
    }

    private DamageReportDetailResponse toDetail(DamageReport dr) {
        return DamageReportDetailResponse.fromEntity(dr, loadAttachments(dr));
    }

    private List<Attachment> loadAttachments(DamageReport dr) {
        List<UUID> itemIds = dr.getItems() == null ? Collections.emptyList()
                : dr.getItems().stream().map(DamageItem::getId).toList();
        if (itemIds.isEmpty()) {
            return List.of();
        }
        return attachmentRepository.findByEntityTypeAndEntityIdIn(ATTACHMENT_ENTITY_TYPE, itemIds);
    }

    private DamageReport loadPendingForManager(User manager, UUID id) {
        DamageReport dr = damageReportRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo hư hại"));
        scopeValidator.validateManagerAccess(manager, dr.getInspection().getProperty().getId());
        if (dr.getStatus() != DamageReport.Status.PENDING_APPROVAL) {
            throw new BusinessException("Chỉ có thể xử lý báo cáo đang ở trạng thái Chờ duyệt");
        }
        return dr;
    }

    private void notifyCustomer(DamageReport dr, String message) {
        if (dr.getBooking() != null && dr.getBooking().getCustomer() != null) {
            notificationService.sendNotification(
                    dr.getBooking().getCustomer().getId(),
                    Notification.Type.SYSTEM,
                    "Cập nhật báo cáo hư hại",
                    message,
                    dr.getId(),
                    "DamageReport");
        }
    }

    private void notifyAdmins(DamageReport dr) {
        for (User admin : userRepository.findByRole(User.Role.ADMIN)) {
            notificationService.sendNotification(
                    admin.getId(),
                    Notification.Type.SYSTEM,
                    "Báo cáo hư hại cần Admin duyệt",
                    "Có báo cáo hư hại vượt hạn mức cần Admin đồng phê duyệt.",
                    dr.getId(),
                    "DamageReport");
        }
    }
}
