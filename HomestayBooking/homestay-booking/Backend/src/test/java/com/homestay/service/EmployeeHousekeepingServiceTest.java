package com.homestay.service;

import com.homestay.entity.HousekeepingTask;
import com.homestay.entity.ManagerPropertyAssignment;
import com.homestay.entity.Notification;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.HousekeepingTaskRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeHousekeepingServiceTest {

    @Mock HousekeepingTaskRepository housekeepingTaskRepository;
    @Mock ManagerPropertyAssignmentRepository managerPropertyAssignmentRepository;
    @Mock NotificationService notificationService;

    @InjectMocks EmployeeHousekeepingService service;

    private User employee;
    private User manager;
    private Property property;
    private Room room;
    private HousekeepingTask task;

    @BeforeEach
    void setUp() {
        employee = TestFixtures.user(User.Role.EMPLOYEE);
        manager = TestFixtures.user(User.Role.MANAGER);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
        room.setStatus(Room.Status.PENDING_CLEANING);

        task = new HousekeepingTask();
        task.setId(UUID.randomUUID());
        task.setProperty(property);
        task.setRoom(room);
        task.setAssignedEmployee(employee);
        task.setStatus(HousekeepingTask.Status.PENDING);
        task.setNote("Post-checkout clean");
    }

    @Test
    void start_movesPendingToInProgress_andSetsRoomCleaning() {
        when(housekeepingTaskRepository.findByIdAndAssignedEmployeeId(task.getId(), employee.getId()))
                .thenReturn(Optional.of(task));
        when(housekeepingTaskRepository.save(any(HousekeepingTask.class))).thenAnswer(inv -> inv.getArgument(0));
        stubManagerNotify();

        service.start(employee, task.getId());

        assertEquals(HousekeepingTask.Status.IN_PROGRESS, task.getStatus());
        assertNotNull(task.getStartedAt());
        assertEquals(Room.Status.CLEANING_IN_PROGRESS, room.getStatus());
        verifyNotify(HousekeepingTask.Status.IN_PROGRESS);
    }

    @Test
    void finish_movesInProgressToCompleted_andSetsRoomAvailable() {
        task.setStatus(HousekeepingTask.Status.IN_PROGRESS);
        room.setStatus(Room.Status.CLEANING_IN_PROGRESS);
        when(housekeepingTaskRepository.findByIdAndAssignedEmployeeId(task.getId(), employee.getId()))
                .thenReturn(Optional.of(task));
        when(housekeepingTaskRepository.save(any(HousekeepingTask.class))).thenAnswer(inv -> inv.getArgument(0));
        stubManagerNotify();

        service.finish(employee, task.getId());

        assertEquals(HousekeepingTask.Status.COMPLETED, task.getStatus());
        assertNotNull(task.getCompletedAt());
        assertEquals(Room.Status.AVAILABLE, room.getStatus());
        verifyNotify(HousekeepingTask.Status.COMPLETED);
    }

    @Test
    void start_rejectsNonPending() {
        task.setStatus(HousekeepingTask.Status.IN_PROGRESS);
        when(housekeepingTaskRepository.findByIdAndAssignedEmployeeId(task.getId(), employee.getId()))
                .thenReturn(Optional.of(task));

        assertThrows(BusinessException.class, () -> service.start(employee, task.getId()));
        verify(housekeepingTaskRepository, never()).save(any());
    }

    @Test
    void finish_rejectsWithoutStart() {
        when(housekeepingTaskRepository.findByIdAndAssignedEmployeeId(task.getId(), employee.getId()))
                .thenReturn(Optional.of(task));

        assertThrows(BusinessException.class, () -> service.finish(employee, task.getId()));
        verify(housekeepingTaskRepository, never()).save(any());
    }

    @Test
    void start_rejectsUnassignedTask() {
        when(housekeepingTaskRepository.findByIdAndAssignedEmployeeId(task.getId(), employee.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.start(employee, task.getId()));
    }

    @Test
    void finish_rejectsWrongAssignee() {
        when(housekeepingTaskRepository.findByIdAndAssignedEmployeeId(task.getId(), employee.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.finish(employee, task.getId()));
        verify(housekeepingTaskRepository, never()).save(any());
    }

    @Test
    void finish_rejectsCompleted() {
        task.setStatus(HousekeepingTask.Status.COMPLETED);
        when(housekeepingTaskRepository.findByIdAndAssignedEmployeeId(task.getId(), employee.getId()))
                .thenReturn(Optional.of(task));

        assertThrows(BusinessException.class, () -> service.finish(employee, task.getId()));
        verify(housekeepingTaskRepository, never()).save(any());
    }

    @Test
    void start_rejectsCancelled() {
        task.setStatus(HousekeepingTask.Status.CANCELLED);
        when(housekeepingTaskRepository.findByIdAndAssignedEmployeeId(task.getId(), employee.getId()))
                .thenReturn(Optional.of(task));

        assertThrows(BusinessException.class, () -> service.start(employee, task.getId()));
        verify(housekeepingTaskRepository, never()).save(any());
    }

    private void stubManagerNotify() {
        ManagerPropertyAssignment mpa = new ManagerPropertyAssignment();
        mpa.setManager(manager);
        mpa.setProperty(property);
        mpa.setStatus(ManagerPropertyAssignment.Status.ACTIVE);
        when(managerPropertyAssignmentRepository.findActiveByPropertyIds(
                eq(List.of(property.getId())), eq(ManagerPropertyAssignment.Status.ACTIVE)))
                .thenReturn(List.of(mpa));
    }

    private void verifyNotify(HousekeepingTask.Status expectedStatus) {
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendNotification(
                eq(manager.getId()),
                eq(Notification.Type.HOUSEKEEPING_TASK_UPDATED),
                eq("Housekeeping updated"),
                contentCaptor.capture(),
                eq(task.getId()),
                eq("HousekeepingTask"));
        assertEquals(
                String.format("Room %s housekeeping is now %s.", room.getRoomNumber(), expectedStatus.name()),
                contentCaptor.getValue());
    }
}
