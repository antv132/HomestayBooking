package com.homestay.service;

import com.homestay.dto.response.AdminDamageReportResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SCR-53 - Damage Escalation (Admin). Read hang doi escalated + co-approve.
 * KHONG dung SCR-43 (Manager) / SCR-64-63 (Employee) / luong Customer.
 */
@Service
@RequiredArgsConstructor
public class AdminDamageService {

    private static final String ATTACHMENT_ENTITY_TYPE = "DamageItem";

    private final DamageReportRepository damageReportRepository;
    private final AttachmentRepository attachmentRepository;
    private final NotificationService notificationService;
    private final DamageFeeSettlementService damageFeeSettlementService;

    @Transactional(readOnly = true)
    public PageResponse<AdminDamageReportResponse> listEscalated(Pageable pageable) {
        Page<DamageReport> page = damageReportRepository.findEscalatedForAdmin(
                DamageReport.Status.PENDING_APPROVAL, pageable);

        // Gom tat ca damage item id cua ca trang -> 1 query attachment (tranh N+1).
        List<UUID> allItemIds = page.getContent().stream()
                .flatMap(dr -> safeItems(dr).stream())
                .map(DamageItem::getId)
                .toList();

        Map<UUID, List<Attachment>> attByItemId = allItemIds.isEmpty()
                ? Map.of()
                : attachmentRepository
                    .findByEntityTypeAndEntityIdIn(ATTACHMENT_ENTITY_TYPE, allItemIds)
                    .stream()
                    .collect(Collectors.groupingBy(Attachment::getEntityId));

        List<AdminDamageReportResponse> content = page.getContent().stream()
                .map(dr -> AdminDamageReportResponse.from(dr, collectAttachments(dr, attByItemId)))
                .toList();

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional
    public AdminDamageReportResponse coApprove(UUID id, BigDecimal approvedFee, User admin) {
        DamageReport dr = damageReportRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay bao cao hu hai"));

        if (!Boolean.TRUE.equals(dr.getRequiresAdminEscalation())
                || dr.getStatus() != DamageReport.Status.PENDING_APPROVAL) {
            throw new BusinessException("Bao cao khong o trang thai cho Admin duyet");
        }
        if (dr.getApprovedBy() == null) {
            throw new BusinessException("Bao cao chua duoc Manager escalate — khong the co-approve");
        }
        if (approvedFee == null || approvedFee.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("So tien duyet phai lon hon 0");
        }

        dr.setStatus(DamageReport.Status.APPROVED);
        dr.setAdminApprover(admin);
        dr.setApprovedAmount(approvedFee);
        if (dr.getApprovedAt() == null) {
            dr.setApprovedAt(LocalDateTime.now());
        }

        damageFeeSettlementService.applyApprovedFee(dr, approvedFee);

        DamageReport saved = damageReportRepository.save(dr);

        notifyCustomer(saved);

        return AdminDamageReportResponse.from(saved, collectAttachments(saved, null));
    }

    private List<DamageItem> safeItems(DamageReport dr) {
        return dr.getItems() == null ? Collections.emptyList() : dr.getItems();
    }

    // Gop attachment cua tat ca item thuoc report. Neu map == null (co-approve don le) -> query truc tiep.
    private List<Attachment> collectAttachments(DamageReport dr, Map<UUID, List<Attachment>> attByItemId) {
        List<UUID> itemIds = safeItems(dr).stream().map(DamageItem::getId).toList();
        if (itemIds.isEmpty()) {
            return List.of();
        }
        if (attByItemId == null) {
            return attachmentRepository.findByEntityTypeAndEntityIdIn(ATTACHMENT_ENTITY_TYPE, itemIds);
        }
        List<Attachment> result = new ArrayList<>();
        for (UUID itemId : itemIds) {
            List<Attachment> atts = attByItemId.get(itemId);
            if (atts != null) {
                result.addAll(atts);
            }
        }
        return result;
    }

    private void notifyCustomer(DamageReport dr) {
        if (dr.getBooking() != null && dr.getBooking().getCustomer() != null) {
            notificationService.sendNotification(
                    dr.getBooking().getCustomer().getId(),
                    Notification.Type.SYSTEM,
                    "Cap nhat bao cao hu hai",
                    "Phi boi thuong da duoc Admin duyet.",
                    dr.getId(),
                    "DamageReport");
        }
    }
}