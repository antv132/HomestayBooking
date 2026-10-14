package com.homestay.service;

import com.homestay.dto.response.EmployeeRoomResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.StaffRoomMonthCalendarResponse;
import com.homestay.entity.EmployeePropertyAssignment;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.ForbiddenException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.EmployeePropertyAssignmentRepository;
import com.homestay.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** SCR-65 / FR-05 US4 — Property room list + read-only calendar (assigned properties only). */
@Service
@RequiredArgsConstructor
public class EmployeeRoomService {

    private final RoomRepository roomRepository;
    private final EmployeePropertyAssignmentRepository employeePropertyAssignmentRepository;
    private final RoomService roomService;

    @Transactional(readOnly = true)
    public PageResponse<EmployeeRoomResponse> list(User employee, Pageable pageable) {
        List<UUID> propertyIds = assignedPropertyIds(employee.getId());

        if (propertyIds.isEmpty()) {
            return new PageResponse<>(
                    List.of(),
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    0L,
                    0);
        }

        Page<Room> page = roomRepository.findWithFiltersInProperties(
                null, null, propertyIds, null, null, pageable);

        List<EmployeeRoomResponse> content = page.getContent().stream()
                .map(EmployeeRoomResponse::fromEntity)
                .toList();

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    /** FR-05 US4 — read-only month calendar; same days[] as public/manager. */
    @Transactional(readOnly = true)
    public StaffRoomMonthCalendarResponse getMonthCalendar(
            User employee, UUID roomId, LocalDate startDate, LocalDate endDate) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng"));
        if (room.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Không tìm thấy phòng");
        }
        List<UUID> propertyIds = assignedPropertyIds(employee.getId());
        if (propertyIds.isEmpty() || !propertyIds.contains(room.getProperty().getId())) {
            throw new ForbiddenException("Bạn không được gán property của phòng này");
        }
        return roomService.buildStaffMonthCalendar(room, startDate, endDate);
    }

    private List<UUID> assignedPropertyIds(UUID employeeId) {
        return employeePropertyAssignmentRepository.findPropertyIdsByEmployeeIdAndStatus(
                employeeId, EmployeePropertyAssignment.Status.ACTIVE);
    }
}
