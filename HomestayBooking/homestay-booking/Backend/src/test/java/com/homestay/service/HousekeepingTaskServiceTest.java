package com.homestay.service;

import com.homestay.dto.request.AssignHousekeepingTaskRequest;
import com.homestay.dto.request.CancelHousekeepingTaskRequest;
import com.homestay.dto.request.CreateHousekeepingTaskRequest;
import com.homestay.entity.Booking;
import com.homestay.entity.EmployeePropertyAssignment;
import com.homestay.entity.HousekeepingTask;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ConflictException;
import com.homestay.repository.EmployeePropertyAssignmentRepository;
import com.homestay.repository.HousekeepingTaskRepository;
import com.homestay.repository.RoomRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HousekeepingTaskServiceTest {

    @Mock HousekeepingTaskRepository taskRepository;
    @Mock EmployeePropertyAssignmentRepository employeeAssignmentRepository;
    @Mock RoomRepository roomRepository;
    @Mock ReportPropertyScopeValidator scopeValidator;

    private HousekeepingTaskService service;
    private User manager;
    private User employee;
    private Property property;
    private Room room;
    private Booking booking;

    @BeforeEach
    void setUp() {
        service = new HousekeepingTaskService(
                taskRepository, employeeAssignmentRepository, roomRepository, scopeValidator);
        manager = TestFixtures.user(User.Role.MANAGER);
        employee = TestFixtures.user(User.Role.EMPLOYEE);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
        room.setStatus(Room.Status.PENDING_CLEANING);
        booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setRoom(room);
    }

    @Test
    void onBookingCheckedOut_createsPendingTask() {
        when(taskRepository.existsByBooking_Id(booking.getId())).thenReturn(false);
        when(taskRepository.save(any(HousekeepingTask.class))).thenAnswer(inv -> inv.getArgument(0));

        service.onBookingCheckedOut(booking);

        ArgumentCaptor<HousekeepingTask> cap = ArgumentCaptor.forClass(HousekeepingTask.class);
        verify(taskRepository).save(cap.capture());
        assertEquals(HousekeepingTask.Status.PENDING, cap.getValue().getStatus());
        assertEquals(booking.getId(), cap.getValue().getBooking().getId());
    }

    @Test
    void onBookingCheckedOut_idempotent() {
        when(taskRepository.existsByBooking_Id(booking.getId())).thenReturn(true);
        service.onBookingCheckedOut(booking);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTaskForManager_rejectsWrongRoomStatus() {
        room.setStatus(Room.Status.AVAILABLE);
        CreateHousekeepingTaskRequest req = new CreateHousekeepingTaskRequest();
        req.setRoomId(room.getId());
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));

        assertThrows(BusinessException.class, () -> service.createTaskForManager(manager, req));
    }

    @Test
    void createTaskForManager_conflictWhenOpenExists() {
        CreateHousekeepingTaskRequest req = new CreateHousekeepingTaskRequest();
        req.setRoomId(room.getId());
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(taskRepository.findOpenByRoomId(eq(room.getId()), any()))
                .thenReturn(Optional.of(new HousekeepingTask()));

        assertThrows(ConflictException.class, () -> service.createTaskForManager(manager, req));
    }

    @Test
    void createTaskForManager_success() {
        CreateHousekeepingTaskRequest req = new CreateHousekeepingTaskRequest();
        req.setRoomId(room.getId());
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(taskRepository.findOpenByRoomId(eq(room.getId()), any())).thenReturn(Optional.empty());
        when(taskRepository.save(any(HousekeepingTask.class))).thenAnswer(inv -> {
            HousekeepingTask t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        var res = service.createTaskForManager(manager, req);
        assertEquals(HousekeepingTask.Status.PENDING.name(), res.getStatus());
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
    }

    @Test
    void cancelTaskForManager_onlyOpen() {
        HousekeepingTask task = openTask();
        task.setStatus(HousekeepingTask.Status.COMPLETED);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        assertThrows(ConflictException.class, () ->
                service.cancelTaskForManager(manager, task.getId(), new CancelHousekeepingTaskRequest()));
    }

    @Test
    void cancelTaskForManager_revertsCleaningInProgress() {
        HousekeepingTask task = openTask();
        task.setStatus(HousekeepingTask.Status.IN_PROGRESS);
        room.setStatus(Room.Status.CLEANING_IN_PROGRESS);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CancelHousekeepingTaskRequest req = new CancelHousekeepingTaskRequest();
        req.setNote("Guest delayed checkout");
        service.cancelTaskForManager(manager, task.getId(), req);

        assertEquals(HousekeepingTask.Status.CANCELLED, task.getStatus());
        assertEquals(Room.Status.PENDING_CLEANING, room.getStatus());
    }

    @Test
    void assignTaskForManager_rejectsCrossPropertyEmployee() {
        HousekeepingTask task = openTask();
        UUID foreignEmployeeId = UUID.randomUUID();
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(employeeAssignmentRepository.existsByEmployeeIdAndPropertyIdAndStatus(
                foreignEmployeeId, property.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(false);

        AssignHousekeepingTaskRequest req = new AssignHousekeepingTaskRequest();
        req.setAssigneeId(foreignEmployeeId);

        assertThrows(BusinessException.class, () ->
                service.assignTaskForManager(manager, task.getId(), req));
    }

    @Test
    void assignTaskForManager_success() {
        HousekeepingTask task = openTask();
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(employeeAssignmentRepository.existsByEmployeeIdAndPropertyIdAndStatus(
                employee.getId(), property.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(true);
        when(employeeAssignmentRepository.findActiveEmployeesByPropertyId(
                property.getId(), EmployeePropertyAssignment.Status.ACTIVE, User.Role.EMPLOYEE))
                .thenReturn(List.of(employee));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssignHousekeepingTaskRequest req = new AssignHousekeepingTaskRequest();
        req.setAssigneeId(employee.getId());
        service.assignTaskForManager(manager, task.getId(), req);

        assertEquals(employee.getId(), task.getAssignedEmployee().getId());
    }

    @Test
    void listTasksForManager_validatesScope() {
        when(taskRepository.findForManagerBoard(eq(property.getId()), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listTasksForManager(manager, property.getId(), null, null, null, 0, 10);
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
    }

    @Test
    void listTasksForAdmin() {
        HousekeepingTask task = openTask();
        when(taskRepository.findForAdmin(eq(property.getId()), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));
        assertEquals(1, service.listTasksForAdmin(property.getId(), null, 0, 10).getContent().size());
    }

    @Test
    void getScheduleForManager_buildsColumns() {
        HousekeepingTask unassigned = openTask();
        HousekeepingTask assigned = openTask();
        assigned.setStatus(HousekeepingTask.Status.IN_PROGRESS);
        assigned.setAssignedEmployee(employee);
        HousekeepingTask done = openTask();
        done.setStatus(HousekeepingTask.Status.COMPLETED);
        done.setAssignedEmployee(employee);

        when(taskRepository.findScheduleTasks(eq(property.getId()), any(), any(), any()))
                .thenReturn(List.of(unassigned, assigned, done));
        when(employeeAssignmentRepository.findActiveEmployeesByPropertyId(
                property.getId(), EmployeePropertyAssignment.Status.ACTIVE, User.Role.EMPLOYEE))
                .thenReturn(List.of(employee));

        var schedule = service.getScheduleForManager(manager, property.getId(), java.time.LocalDate.now());
        assertEquals(1, schedule.getSummary().getPending());
        assertEquals(1, schedule.getSummary().getInProgress());
        assertEquals(1, schedule.getSummary().getCompleted());
        assertEquals(1, schedule.getUnassigned().size());
        assertEquals(1, schedule.getEmployees().size());
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
    }

    private HousekeepingTask openTask() {
        HousekeepingTask task = new HousekeepingTask();
        task.setId(UUID.randomUUID());
        task.setProperty(property);
        task.setRoom(room);
        task.setStatus(HousekeepingTask.Status.PENDING);
        return task;
    }
}
