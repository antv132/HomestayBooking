package com.homestay.service;

import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.StaffRoomMonthCalendarResponse;
import com.homestay.entity.EmployeePropertyAssignment;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.ForbiddenException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.EmployeePropertyAssignmentRepository;
import com.homestay.repository.RoomRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeRoomServiceTest {

    @Mock RoomRepository roomRepository;
    @Mock EmployeePropertyAssignmentRepository assignmentRepository;
    @Mock RoomService roomService;

    private EmployeeRoomService service;
    private User employee;
    private Property property;
    private Room room;

    @BeforeEach
    void setUp() {
        service = new EmployeeRoomService(roomRepository, assignmentRepository, roomService);
        employee = TestFixtures.user(User.Role.EMPLOYEE);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
        room.setDeletedAt(null);
    }

    @Test
    void list_emptyAssignments() {
        when(assignmentRepository.findPropertyIdsByEmployeeIdAndStatus(
                employee.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of());
        PageResponse<?> page = service.list(employee, PageRequest.of(0, 10));
        assertEquals(0, page.getTotalElements());
    }

    @Test
    void list_scopedRooms() {
        when(assignmentRepository.findPropertyIdsByEmployeeIdAndStatus(
                employee.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of(property.getId()));
        when(roomRepository.findWithFiltersInProperties(
                eq(null), eq(null), eq(List.of(property.getId())), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(room)));

        assertEquals(1, service.list(employee, PageRequest.of(0, 10)).getContent().size());
    }

    @Test
    void getMonthCalendar_forbiddenOutsideScope() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(assignmentRepository.findPropertyIdsByEmployeeIdAndStatus(
                employee.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of(UUID.randomUUID()));

        assertThrows(ForbiddenException.class, () ->
                service.getMonthCalendar(employee, room.getId(), LocalDate.now(), LocalDate.now().plusDays(7)));
    }

    @Test
    void getMonthCalendar_deletedRoom() {
        room.setDeletedAt(LocalDateTime.now());
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        assertThrows(ResourceNotFoundException.class, () ->
                service.getMonthCalendar(employee, room.getId(), LocalDate.now(), LocalDate.now().plusDays(1)));
    }

    @Test
    void getMonthCalendar_delegates() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(assignmentRepository.findPropertyIdsByEmployeeIdAndStatus(
                employee.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of(property.getId()));
        StaffRoomMonthCalendarResponse calendar = new StaffRoomMonthCalendarResponse();
        when(roomService.buildStaffMonthCalendar(eq(room), any(), any())).thenReturn(calendar);

        LocalDate start = LocalDate.now().withDayOfMonth(1);
        assertEquals(calendar, service.getMonthCalendar(employee, room.getId(), start, start.plusDays(30)));
        verify(roomService).buildStaffMonthCalendar(eq(room), eq(start), any());
    }
}
