package com.homestay.service;

import com.homestay.dto.response.EmployeeDashboardResponse;
import com.homestay.dto.response.EmployeeKpisResponse;
import com.homestay.entity.EmployeePropertyAssignment;
import com.homestay.entity.HousekeepingTask;
import com.homestay.entity.MaintenanceTicket;
import com.homestay.entity.RoomInspection;
import com.homestay.entity.User;
import com.homestay.repository.EmployeePropertyAssignmentRepository;
import com.homestay.repository.HousekeepingTaskRepository;
import com.homestay.repository.MaintenanceTicketRepository;
import com.homestay.repository.RoomInspectionRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeDashboardServiceTest {

    @Mock HousekeepingTaskRepository housekeepingTaskRepository;
    @Mock MaintenanceTicketRepository maintenanceTicketRepository;
    @Mock RoomInspectionRepository roomInspectionRepository;
    @Mock EmployeePropertyAssignmentRepository employeePropertyAssignmentRepository;

    private EmployeeDashboardService service;
    private User employee;

    @BeforeEach
    void setUp() {
        service = new EmployeeDashboardService(
                housekeepingTaskRepository,
                maintenanceTicketRepository,
                roomInspectionRepository,
                employeePropertyAssignmentRepository);
        employee = TestFixtures.user(User.Role.EMPLOYEE);
        employee.setFullName("Nguyễn Văn Emp");
    }

    @Test
    void getDashboard_noPropertyAssignment_returnsEmptyFlags() {
        when(employeePropertyAssignmentRepository.findPropertyIdsByEmployeeIdAndStatus(
                employee.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(Collections.emptyList());

        EmployeeDashboardResponse res = service.getDashboard(employee);

        assertTrue(res.isNoPropertyAssignment());
        assertEquals("Nguyễn Văn Emp", res.getFullName());
        assertEquals(0, res.getPendingHousekeeping());
        assertEquals(0, res.getPendingMaintenance());
        assertEquals(0, res.getPendingInspections());
        assertTrue(res.getHousekeepingTasks().isEmpty());
        assertTrue(res.getTodayTasks().isEmpty());
    }

    @Test
    void getDashboard_withAssignments_mapsCountsAndLimits() {
        UUID propertyId = UUID.randomUUID();
        when(employeePropertyAssignmentRepository.findPropertyIdsByEmployeeIdAndStatus(
                employee.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of(propertyId));

        when(housekeepingTaskRepository.countForDashboard(eq(employee.getId()), any(), any()))
                .thenReturn(3L);
        when(maintenanceTicketRepository.countForDashboard(eq(employee.getId()), any(), any()))
                .thenReturn(2L);
        when(roomInspectionRepository.countForEmployeeDashboard(any(), any(), eq(employee.getId())))
                .thenReturn(1L);

        when(housekeepingTaskRepository.findPreviewForDashboard(eq(employee.getId()), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());
        when(maintenanceTicketRepository.findPreviewForDashboard(eq(employee.getId()), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());
        when(roomInspectionRepository.findForEmployee(any(), any(), eq(employee.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        when(housekeepingTaskRepository.countCompletedToday(eq(employee.getId()), any(), any(), any()))
                .thenReturn(1L);
        when(maintenanceTicketRepository.countCompletedToday(eq(employee.getId()), any(), any(), any()))
                .thenReturn(0L);
        when(roomInspectionRepository.countCompletedToday(eq(employee.getId()), any(), any(), any()))
                .thenReturn(0L);

        when(housekeepingTaskRepository.findCompletedToday(eq(employee.getId()), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());
        when(maintenanceTicketRepository.findCompletedToday(eq(employee.getId()), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());
        when(roomInspectionRepository.findCompletedToday(eq(employee.getId()), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        when(housekeepingTaskRepository.findTodayForDashboard(eq(employee.getId()), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());
        when(maintenanceTicketRepository.findTodayForDashboard(eq(employee.getId()), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());
        when(roomInspectionRepository.findTodayForDashboard(any(), eq(employee.getId()), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        EmployeeDashboardResponse res = service.getDashboard(employee);

        assertFalse(res.isNoPropertyAssignment());
        assertEquals(3, res.getPendingHousekeeping());
        assertEquals(2, res.getPendingMaintenance());
        assertEquals(1, res.getPendingInspections());
        assertEquals(3, res.getAwaiting().getHousekeepingCount());
        assertEquals(1, res.getCompletedToday().getHousekeepingCount());

        EmployeeKpisResponse kpis = service.getKpis(employee);
        assertEquals(3, kpis.getPendingHousekeeping());
        assertEquals(2, kpis.getPendingMaintenance());
        assertEquals(1, kpis.getPendingInspections());
    }

    @Test
    void getDashboard_openStatusesMatchContract() {
        UUID propertyId = UUID.randomUUID();
        when(employeePropertyAssignmentRepository.findPropertyIdsByEmployeeIdAndStatus(
                employee.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of(propertyId));

        when(housekeepingTaskRepository.countForDashboard(
                eq(employee.getId()),
                eq(List.of(propertyId)),
                eq(List.of(HousekeepingTask.Status.PENDING, HousekeepingTask.Status.IN_PROGRESS))))
                .thenReturn(1L);
        when(maintenanceTicketRepository.countForDashboard(
                eq(employee.getId()),
                eq(List.of(propertyId)),
                eq(List.of(MaintenanceTicket.Status.ASSIGNED, MaintenanceTicket.Status.IN_PROGRESS))))
                .thenReturn(1L);
        when(roomInspectionRepository.countForEmployeeDashboard(
                eq(List.of(propertyId)),
                eq(List.of(RoomInspection.Status.PENDING, RoomInspection.Status.IN_PROGRESS)),
                eq(employee.getId())))
                .thenReturn(1L);

        when(housekeepingTaskRepository.findPreviewForDashboard(any(), any(), any(), any())).thenReturn(List.of());
        when(maintenanceTicketRepository.findPreviewForDashboard(any(), any(), any(), any())).thenReturn(List.of());
        when(roomInspectionRepository.findForEmployee(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(housekeepingTaskRepository.countCompletedToday(any(), any(), any(), any())).thenReturn(0L);
        when(maintenanceTicketRepository.countCompletedToday(any(), any(), any(), any())).thenReturn(0L);
        when(roomInspectionRepository.countCompletedToday(any(), any(), any(), any())).thenReturn(0L);
        when(housekeepingTaskRepository.findCompletedToday(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(maintenanceTicketRepository.findCompletedToday(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(roomInspectionRepository.findCompletedToday(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(housekeepingTaskRepository.findTodayForDashboard(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(maintenanceTicketRepository.findTodayForDashboard(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(roomInspectionRepository.findTodayForDashboard(any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        EmployeeDashboardResponse res = service.getDashboard(employee);
        assertEquals(1, res.getPendingHousekeeping());
        assertEquals(1, res.getPendingMaintenance());
        assertEquals(1, res.getPendingInspections());
    }
}
