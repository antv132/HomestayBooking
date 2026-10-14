package com.homestay.service;

import com.homestay.dto.request.UpdateMaintenanceStatusRequest;
import com.homestay.dto.response.EmployeeMaintenanceTicketResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.MaintenanceTicket;
import com.homestay.entity.Notification;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.MaintenanceTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * SCR-61 — Employee Maintenance Workspace.
 * List assigned tickets; update status ASSIGNED → IN_PROGRESS → RESOLVED only.
 */
@Service
@RequiredArgsConstructor
public class EmployeeMaintenanceService {

    private static final Map<MaintenanceTicket.Status, Set<MaintenanceTicket.Status>> ALLOWED_TRANSITIONS = Map.of(
            MaintenanceTicket.Status.ASSIGNED, EnumSet.of(MaintenanceTicket.Status.IN_PROGRESS),
            MaintenanceTicket.Status.IN_PROGRESS, EnumSet.of(MaintenanceTicket.Status.RESOLVED)
    );

    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public PageResponse<EmployeeMaintenanceTicketResponse> list(
            User employee, String statusStr, Pageable pageable) {
        MaintenanceTicket.Status status = parseStatus(statusStr);
        Page<MaintenanceTicket> page = maintenanceTicketRepository.findForEmployee(
                employee.getId(), status, pageable);
        List<EmployeeMaintenanceTicketResponse> content = page.getContent().stream()
                .map(EmployeeMaintenanceTicketResponse::fromEntity)
                .toList();
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional
    public EmployeeMaintenanceTicketResponse updateStatus(
            User employee, UUID ticketId, UpdateMaintenanceStatusRequest request) {
        MaintenanceTicket ticket = maintenanceTicketRepository
                .findByIdAndAssignedEmployeeId(ticketId, employee.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy yêu cầu bảo trì hoặc bạn không được giao việc này"));

        if (ticket.getStatus() == MaintenanceTicket.Status.CLOSED) {
            throw new BusinessException("Yêu cầu đã đóng — không thể cập nhật");
        }

        MaintenanceTicket.Status current = ticket.getStatus();
        MaintenanceTicket.Status next = request.getStatus();

        Set<MaintenanceTicket.Status> allowed = ALLOWED_TRANSITIONS.getOrDefault(
                current, EnumSet.noneOf(MaintenanceTicket.Status.class));
        if (!allowed.contains(next)) {
            throw new BusinessException(
                    "Không thể chuyển trạng thái từ " + current + " sang " + next);
        }

        ticket.setStatus(next);

        if (next == MaintenanceTicket.Status.RESOLVED) {
            String note = request.getResolutionNote() != null
                    ? request.getResolutionNote().trim()
                    : "";
            if (!StringUtils.hasText(note)) {
                throw new BusinessException(
                        "Vui lòng ghi chú vật tư / cách xử lý khi đánh dấu đã xử lý");
            }
            // Internal employee note — not shown to customer until manager closes
            ticket.setWorkNote(note);
        }

        MaintenanceTicket saved = maintenanceTicketRepository.save(ticket);

        activityLogService.log(employee, ActivityLogService.TICKET_STATUS_CHANGED, "MaintenanceTicket",
                saved.getId(), current + " → " + next);

        String msg = next == MaintenanceTicket.Status.IN_PROGRESS
                ? "Yêu cầu bảo trì '%s' đang được xử lý."
                : "Yêu cầu bảo trì '%s' đã được kỹ thuật viên đánh dấu hoàn tất — chờ quản lý xác nhận.";
        notificationService.sendNotification(
                saved.getCustomer().getId(),
                Notification.Type.MAINTENANCE_UPDATED,
                "Cập nhật yêu cầu bảo trì",
                String.format(msg, saved.getTitle()),
                saved.getId(),
                "MaintenanceTicket");

        return EmployeeMaintenanceTicketResponse.fromEntity(saved);
    }

    private MaintenanceTicket.Status parseStatus(String statusStr) {
        if (!StringUtils.hasText(statusStr)) {
            return null;
        }
        try {
            return MaintenanceTicket.Status.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Trạng thái không hợp lệ");
        }
    }
}
