package com.homestay.service;

import com.homestay.dto.response.EmployeeHousekeepingTaskResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.HousekeepingTask;
import com.homestay.entity.ManagerPropertyAssignment;
import com.homestay.entity.Notification;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.HousekeepingTaskRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * SCR-60 - Employee Housekeeping Workspace.
 * List assigned tasks; Start (PENDING -> IN_PROGRESS) and Finish (IN_PROGRESS -> COMPLETED).
 * Updates Room status: CLEANING_IN_PROGRESS on start, AVAILABLE on finish.
 */
@Service
@RequiredArgsConstructor
public class EmployeeHousekeepingService {

    private final HousekeepingTaskRepository housekeepingTaskRepository;
    private final ManagerPropertyAssignmentRepository managerPropertyAssignmentRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public PageResponse<EmployeeHousekeepingTaskResponse> list(
            User employee, String statusStr, Pageable pageable) {
        HousekeepingTask.Status status = parseStatus(statusStr);
        Page<HousekeepingTask> page = housekeepingTaskRepository.findForEmployee(
                employee.getId(), status, pageable);
        List<EmployeeHousekeepingTaskResponse> content = page.getContent().stream()
                .map(EmployeeHousekeepingTaskResponse::fromEntity)
                .toList();
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional
    public void start(User employee, UUID taskId) {
        HousekeepingTask task = loadAssigned(employee.getId(), taskId);
        if (task.getStatus() != HousekeepingTask.Status.PENDING) {
            throw new BusinessException("Chi co the bat dau task o trang thai PENDING");
        }
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(HousekeepingTask.Status.IN_PROGRESS);
        task.setStartedAt(now);
        Room room = task.getRoom();
        room.setStatus(Room.Status.CLEANING_IN_PROGRESS);
        housekeepingTaskRepository.save(task);
        notifyManagers(task);
    }

    @Transactional
    public void finish(User employee, UUID taskId) {
        HousekeepingTask task = loadAssigned(employee.getId(), taskId);
        if (task.getStatus() != HousekeepingTask.Status.IN_PROGRESS) {
            throw new BusinessException("Chi co the hoan thanh task o trang thai IN_PROGRESS");
        }
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(HousekeepingTask.Status.COMPLETED);
        task.setCompletedAt(now);
        Room room = task.getRoom();
        room.setStatus(Room.Status.AVAILABLE);
        housekeepingTaskRepository.save(task);
        notifyManagers(task);
    }

    private void notifyManagers(HousekeepingTask task) {
        UUID propertyId = task.getProperty() != null
                ? task.getProperty().getId()
                : (task.getRoom() != null && task.getRoom().getProperty() != null
                        ? task.getRoom().getProperty().getId()
                        : null);
        if (propertyId == null) {
            return;
        }
        String roomNumber = task.getRoom() != null ? task.getRoom().getRoomNumber() : "?";
        String status = task.getStatus().name();
        List<ManagerPropertyAssignment> managers = managerPropertyAssignmentRepository
                .findActiveByPropertyIds(List.of(propertyId), ManagerPropertyAssignment.Status.ACTIVE);
        for (ManagerPropertyAssignment mpa : managers) {
            if (mpa.getManager() == null) {
                continue;
            }
            notificationService.sendNotification(
                    mpa.getManager().getId(),
                    Notification.Type.HOUSEKEEPING_TASK_UPDATED,
                    "Housekeeping updated",
                    String.format("Room %s housekeeping is now %s.", roomNumber, status),
                    task.getId(),
                    "HousekeepingTask");
        }
    }

    private HousekeepingTask loadAssigned(UUID employeeId, UUID taskId) {
        return housekeepingTaskRepository
                .findByIdAndAssignedEmployeeId(taskId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay housekeeping task"));
    }

    private HousekeepingTask.Status parseStatus(String statusStr) {
        if (!StringUtils.hasText(statusStr)) {
            return null;
        }
        try {
            return HousekeepingTask.Status.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Trang thai khong hop le");
        }
    }
}
