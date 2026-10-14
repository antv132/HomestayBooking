package com.homestay.service;

import com.homestay.dto.response.AdminComplaintResponse;
import com.homestay.dto.response.ComplaintDetailResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.Complaint;
import com.homestay.entity.Notification;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SCR-54 - Complaint Management (Admin). List + status update / resolve.
 */
@Service
@RequiredArgsConstructor
public class AdminComplaintService {

    private final ComplaintRepository complaintRepository;
    private final NotificationService notificationService;
    private final ComplaintStatusValidator statusValidator;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public PageResponse<AdminComplaintResponse> listComplaints(
            String statusStr, String keyword, Pageable pageable) {
        Complaint.Status status = parseStatus(statusStr);
        String search = normalizeSearch(keyword);
        Page<Complaint> page = complaintRepository.findForAdmin(status, search, pageable);
        return new PageResponse<>(
                page.getContent().stream().map(AdminComplaintResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ComplaintDetailResponse getDetail(UUID id) {
        Complaint c = complaintRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khiếu nại"));
        return toDetailResponse(c);
    }

    @Transactional
    public AdminComplaintResponse resolve(UUID id, String resolution, User admin) {
        return updateStatus(id, Complaint.Status.RESOLVED, resolution, admin);
    }

    @Transactional
    public AdminComplaintResponse updateStatus(
            UUID id, Complaint.Status target, String resolution, User admin) {
        Complaint c = complaintRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khiếu nại"));

        Complaint.Status current = c.getStatus();
        statusValidator.validateTransition(current, target);
        statusValidator.validateResolutionNotes(target, resolution, c.getResolutionNotes());

        if (resolution != null && !resolution.isBlank()) {
            c.setResolutionNotes(resolution.trim());
        }

        if (target == Complaint.Status.RESOLVED && c.getResolvedAt() == null) {
            c.setResolvedAt(LocalDateTime.now());
        }

        c.setStatus(target);
        Complaint saved = complaintRepository.save(c);

        activityLogService.log(admin, ActivityLogService.COMPLAINT_STATUS_CHANGED,
                "Complaint", saved.getId(),
                "Status changed from " + current.name() + " to " + target.name());

        Complaint mapped = complaintRepository.findByIdWithUser(saved.getId()).orElse(saved);
        notifyCustomer(mapped, target);
        return AdminComplaintResponse.from(mapped);
    }

    private Complaint.Status parseStatus(String statusStr) {
        if (statusStr == null || statusStr.isBlank() || "ALL".equalsIgnoreCase(statusStr.trim())) {
            return null;
        }
        try {
            return Complaint.Status.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Trạng thái không hợp lệ");
        }
    }

    private static String normalizeSearch(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private void notifyCustomer(Complaint c, Complaint.Status target) {
        if (c.getUser() == null) {
            return;
        }
        String msg = switch (target) {
            case INVESTIGATING -> "Khiếu nại của bạn đang được xem xét.";
            case RESOLVED -> "Khiếu nại của bạn đã được giải quyết.";
            case CLOSED -> "Khiếu nại của bạn đã được đóng.";
            default -> "Khiếu nại của bạn đã được cập nhật.";
        };
        notificationService.sendNotification(
                c.getUser().getId(),
                Notification.Type.SYSTEM,
                "Cập nhật khiếu nại",
                msg,
                c.getId(),
                "Complaint");
    }

    private ComplaintDetailResponse toDetailResponse(Complaint c) {
        ComplaintDetailResponse.CustomerInfo customerInfo = null;
        if (c.getUser() != null) {
            customerInfo = ComplaintDetailResponse.CustomerInfo.builder()
                    .id(c.getUser().getId())
                    .fullName(c.getUser().getFullName())
                    .email(c.getUser().getEmail())
                    .build();
        }
        return ComplaintDetailResponse.builder()
                .id(c.getId())
                .subject(c.getSubject())
                .description(c.getDescription())
                .status(c.getStatus().name())
                .resolutionNotes(c.getResolutionNotes())
                .resolvedAt(c.getResolvedAt())
                .customer(customerInfo)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
