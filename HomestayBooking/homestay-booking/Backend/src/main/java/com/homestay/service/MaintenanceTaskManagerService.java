package com.homestay.service;

import com.homestay.dto.request.AssignMaintenanceTaskRequest;
import com.homestay.dto.request.UpdateMaintenanceStatusRequest;
import com.homestay.dto.response.MaintenanceTaskSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.EmployeePropertyAssignment;
import com.homestay.entity.MaintenanceTicket;
import com.homestay.entity.Notification;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.EmployeePropertyAssignmentRepository;
import com.homestay.repository.MaintenanceTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * SCR-41 — Maintenance Tasks (Manager only).
 * Mọi thao tác đều scope theo property.id qua ReportPropertyScopeValidator.
 */
@Service
@RequiredArgsConstructor
public class MaintenanceTaskManagerService {

    private static final int MIN_RESOLUTION_NOTE_LEN = 10;

    private final MaintenanceTicketRepository ticketRepository;
    private final EmployeePropertyAssignmentRepository employeeAssignmentRepository;
    private final ReportPropertyScopeValidator scopeValidator;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public PageResponse<MaintenanceTaskSummaryResponse> listForManager(
            User manager, UUID propertyId, MaintenanceTicket.Status status, String search, int page, int size) {

        scopeValidator.validateManagerAccess(manager, propertyId);

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        String normalizedSearch = normalizeSearch(search);
        Page<MaintenanceTicket> result =
                ticketRepository.findForManagerBoard(propertyId, status, normalizedSearch, pageable);

        List<MaintenanceTaskSummaryResponse> content = result.getContent().stream()
                .map(MaintenanceTaskSummaryResponse::fromEntity)
                .toList();

        return new PageResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional
    public MaintenanceTaskSummaryResponse assignForManager(User manager, UUID ticketId, AssignMaintenanceTaskRequest req) {
        MaintenanceTicket ticket = ticketRepository.findByIdWithDetails(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu bảo trì"));

        UUID propertyId = ticket.getRoom().getProperty().getId();
        scopeValidator.validateManagerAccess(manager, propertyId);

        if (ticket.getStatus() == MaintenanceTicket.Status.CLOSED) {
            throw new BusinessException("Không thể gán lại yêu cầu đã đóng");
        }
        if (ticket.getStatus() != MaintenanceTicket.Status.OPEN
                && ticket.getStatus() != MaintenanceTicket.Status.ASSIGNED) {
            throw new BusinessException("Chỉ có thể gán kỹ thuật viên khi yêu cầu đang Mở hoặc Đã gán");
        }

        User assignee = resolveActiveEmployee(req.getAssigneeId(), propertyId);

        ticket.setAssignedEmployee(assignee);
        ticket.setAssignedAt(LocalDateTime.now());
        if (ticket.getStatus() == MaintenanceTicket.Status.OPEN) {
            ticket.setStatus(MaintenanceTicket.Status.ASSIGNED);
        }

        MaintenanceTicket saved = ticketRepository.save(ticket);

        activityLogService.log(manager, ActivityLogService.TICKET_ASSIGNED, "MaintenanceTicket",
                saved.getId(), "Assigned to " + assignee.getFullName());
        notifyCustomer(saved, "Yêu cầu bảo trì '%s' đã được gán cho kỹ thuật viên.");

        return MaintenanceTaskSummaryResponse.fromEntity(
                ticketRepository.findByIdWithDetails(saved.getId()).orElse(saved));
    }

    @Transactional
    public MaintenanceTaskSummaryResponse updateStatusForManager(User manager, UUID ticketId, UpdateMaintenanceStatusRequest req) {
        MaintenanceTicket ticket = ticketRepository.findByIdWithDetails(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu bảo trì"));

        scopeValidator.validateManagerAccess(manager, ticket.getRoom().getProperty().getId());

        if (ticket.getStatus() == MaintenanceTicket.Status.CLOSED) {
            throw new BusinessException("Yêu cầu đã đóng — không thể thay đổi trạng thái");
        }

        MaintenanceTicket.Status target = req.getStatus();

        // Manager chỉ được Verify & Close: RESOLVED -> CLOSED.
        if (target != MaintenanceTicket.Status.CLOSED) {
            throw new BusinessException("Quản lý chỉ có thể xác nhận & đóng yêu cầu đã xử lý");
        }
        if (ticket.getStatus() != MaintenanceTicket.Status.RESOLVED) {
            throw new BusinessException("Chỉ có thể đóng yêu cầu đang ở trạng thái Đã xử lý");
        }

        String note = req.getResolutionNote() != null ? req.getResolutionNote().trim() : "";
        if (note.length() < MIN_RESOLUTION_NOTE_LEN) {
            throw new BusinessException("Ghi chú xác nhận đóng phải có ít nhất " + MIN_RESOLUTION_NOTE_LEN + " ký tự");
        }

        ticket.setStatus(MaintenanceTicket.Status.CLOSED);
        ticket.setVerifiedBy(manager);
        ticket.setVerifiedAt(LocalDateTime.now());
        ticket.setResolutionNote(note);

        MaintenanceTicket saved = ticketRepository.save(ticket);

        activityLogService.log(manager, ActivityLogService.TICKET_CLOSED, "MaintenanceTicket",
                saved.getId(), "Closed with note");
        notifyCustomer(saved, "Yêu cầu bảo trì '%s' đã được xác nhận hoàn tất và đóng.");

        return MaintenanceTaskSummaryResponse.fromEntity(
                ticketRepository.findByIdWithDetails(saved.getId()).orElse(saved));
    }

    private User resolveActiveEmployee(UUID assigneeId, UUID propertyId) {
        boolean assigned = employeeAssignmentRepository.existsByEmployeeIdAndPropertyIdAndStatus(
                assigneeId, propertyId, EmployeePropertyAssignment.Status.ACTIVE);
        if (!assigned) {
            throw new BusinessException("Kỹ thuật viên không thuộc homestay này");
        }
        return employeeAssignmentRepository.findActiveEmployeesByPropertyId(
                        propertyId, EmployeePropertyAssignment.Status.ACTIVE, User.Role.EMPLOYEE).stream()
                .filter(e -> e.getId().equals(assigneeId))
                .filter(e -> e.getStatus() == User.Status.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new BusinessException("Chỉ có thể gán kỹ thuật viên đang hoạt động"));
    }

    private void notifyCustomer(MaintenanceTicket ticket, String messageTemplate) {
        notificationService.sendNotification(
                ticket.getCustomer().getId(),
                Notification.Type.MAINTENANCE_UPDATED,
                "Cập nhật yêu cầu bảo trì",
                String.format(messageTemplate, ticket.getTitle()),
                ticket.getId(),
                "MaintenanceTicket");
    }

    private static String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return search.trim();
    }
}
