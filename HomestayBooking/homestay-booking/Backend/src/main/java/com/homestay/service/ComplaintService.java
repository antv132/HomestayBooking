package com.homestay.service;

import com.homestay.dto.request.CreateComplaintRequest;
import com.homestay.dto.request.UpdateComplaintStatusRequest;
import com.homestay.dto.response.ComplaintDetailResponse;
import com.homestay.dto.response.ComplaintSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.Complaint;
import com.homestay.entity.Notification;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.ComplaintRepository;
import com.homestay.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;

    public ComplaintService(ComplaintRepository complaintRepository,
                            UserRepository userRepository,
                            NotificationService notificationService,
                            ActivityLogService activityLogService) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ComplaintSummaryResponse> getAllComplaints(int page, int size, String statusStr, String search) {
        if (size > 100) {
            size = 100;
        }

        Complaint.Status status = null;
        if (statusStr != null && !statusStr.trim().isEmpty() && !statusStr.equalsIgnoreCase("ALL")) {
            try {
                status = Complaint.Status.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Trạng thái khiếu nại không hợp lệ");
            }
        }

        String searchPattern = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Complaint> result = complaintRepository.findByFilters(status, searchPattern, pageable);

        List<ComplaintSummaryResponse> content = result.getContent().stream()
                .map(c -> ComplaintSummaryResponse.builder()
                        .id(c.getId())
                        .subject(c.getSubject())
                        .customerName(c.getUser() != null ? c.getUser().getFullName() : "Khách vãng lai")
                        .status(c.getStatus().name())
                        .createdAt(c.getCreatedAt())
                        .build())
                .toList();

        return new PageResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ComplaintDetailResponse getComplaintDetail(UUID id) {
        Complaint c = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khiếu nại không tồn tại"));
        return toDetailResponse(c);
    }

    @Transactional(readOnly = true)
    public ComplaintDetailResponse getMyComplaintDetail(UUID customerId, UUID id) {
        Complaint c = complaintRepository.findByIdAndUserId(id, customerId)
                .orElseThrow(() -> new BusinessException("Khiếu nại không tồn tại hoặc không thuộc về bạn"));
        return toDetailResponse(c);
    }

    @Transactional(readOnly = true)
    public PageResponse<ComplaintDetailResponse> listMyComplaints(UUID customerId, Pageable pageable) {
        Page<Complaint> page = complaintRepository.findByUserIdOrderByCreatedAtDesc(customerId, pageable);
        List<ComplaintDetailResponse> content = page.getContent().stream()
                .map(this::toDetailResponse)
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<ComplaintDetailResponse> getMyComplaints(UUID customerId) {
        List<Complaint> complaints = complaintRepository.findAll().stream()
                .filter(c -> c.getUser() != null && c.getUser().getId().equals(customerId))
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .toList();
        return complaints.stream().map(this::toDetailResponse).toList();
    }

    @Transactional
    public ComplaintDetailResponse updateComplaintStatus(UUID id, UpdateComplaintStatusRequest request) {
        Complaint c = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khiếu nại không tồn tại"));

        Complaint.Status current = c.getStatus();
        Complaint.Status next = request.getStatus();

        if (current == next) {
            throw new BusinessException("Trạng thái mới trùng với trạng thái hiện tại");
        }

        if (next == Complaint.Status.RESOLVED || next == Complaint.Status.CLOSED) {
            if (request.getResolutionNotes() == null || request.getResolutionNotes().trim().isEmpty()) {
                throw new BusinessException("Ghi chú xử lý là bắt buộc khi giải quyết hoặc đóng khiếu nại");
            }
        }

        if (next == Complaint.Status.RESOLVED) {
            if (c.getResolvedAt() == null) {
                c.setResolvedAt(LocalDateTime.now());
            }
        }

        c.setStatus(next);
        if (request.getResolutionNotes() != null) {
            c.setResolutionNotes(request.getResolutionNotes().trim());
        }

        c = complaintRepository.save(c);
        return toDetailResponse(c);
    }

    @Transactional
    public ComplaintDetailResponse submitComplaint(CreateComplaintRequest request, User currentUser) {
        Complaint c = new Complaint();
        c.setUser(currentUser);
        c.setSubject(request.getSubject().trim());
        c.setDescription(request.getDescription().trim());
        c.setStatus(Complaint.Status.OPEN);
        c = complaintRepository.save(c);

        activityLogService.log(currentUser, ActivityLogService.COMPLAINT_CREATED,
                "Complaint", c.getId(),
                "Customer submitted complaint: " + c.getSubject());

        List<User> admins = userRepository.findByRole(User.Role.ADMIN);
        for (User admin : admins) {
            notificationService.sendNotification(
                    admin.getId(),
                    Notification.Type.SYSTEM,
                    "Khiếu nại mới từ khách hàng",
                    "Khách hàng " + currentUser.getFullName() + " đã gửi khiếu nại mới: " + c.getSubject(),
                    c.getId(),
                    "Complaint"
            );
        }

        return toDetailResponse(c);
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
