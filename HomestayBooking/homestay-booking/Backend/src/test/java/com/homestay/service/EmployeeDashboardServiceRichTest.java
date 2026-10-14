package com.homestay.service;

import com.homestay.dto.response.EmployeeDashboardResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.EmployeePropertyAssignment;
import com.homestay.entity.HousekeepingTask;
import com.homestay.entity.MaintenanceTicket;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeDashboardServiceRichTest {

    @Mock HousekeepingTaskRepository housekeepingTaskRepository;
    @Mock MaintenanceTicketRepository maintenanceTicketRepository;
    @Mock RoomInspectionRepository roomInspectionRepository;
    @Mock EmployeePropertyAssignmentRepository employeePropertyAssignmentRepository;

    private EmployeeDashboardService service;
    private User employee;
    private Property property;
    private Room room;
    private Booking booking;

    @BeforeEach
    void setUp() {
        service = new EmployeeDashboardService(
                housekeepingTaskRepository,
                maintenanceTicketRepository,
                roomInspectionRepository,
                employeePropertyAssignmentRepository);
        employee = TestFixtures.user(User.Role.EMPLOYEE);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
        booking = TestFixtures.booking(TestFixtures.user(User.Role.CUSTOMER), room, Booking.Status.CHECKED_OUT);
    }

    @Test
    void getDashboard_mapsPreviewsAndTodayTasks() {
        UUID propertyId = property.getId();
        when(employeePropertyAssignmentRepository.findPropertyIdsByEmployeeIdAndStatus(
                employee.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of(propertyId));

        HousekeepingTask hk = new HousekeepingTask();
        hk.setId(UUID.randomUUID());
        hk.setRoom(room);
        hk.setProperty(property);
        hk.setBooking(booking);
        hk.setStatus(HousekeepingTask.Status.PENDING);

        MaintenanceTicket mt = new MaintenanceTicket();
        mt.setId(UUID.randomUUID());
        mt.setRoom(room);
        mt.setTitle("Fix AC");
        mt.setStatus(MaintenanceTicket.Status.ASSIGNED);

        RoomInspection ri = TestFixtures.inspection(booking, property, room);
        ri.setStatus(RoomInspection.Status.IN_PROGRESS);

        when(housekeepingTaskRepository.countForDashboard(any(), any(), any())).thenReturn(1L);
        when(maintenanceTicketRepository.countForDashboard(any(), any(), any())).thenReturn(1L);
        when(roomInspectionRepository.countForEmployeeDashboard(any(), any(), any())).thenReturn(1L);

        when(housekeepingTaskRepository.findPreviewForDashboard(any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(hk));
        when(maintenanceTicketRepository.findPreviewForDashboard(any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(mt));
        when(roomInspectionRepository.findForEmployee(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ri)));

        when(housekeepingTaskRepository.countCompletedToday(any(), any(), any(), any())).thenReturn(1L);
        when(maintenanceTicketRepository.countCompletedToday(any(), any(), any(), any())).thenReturn(1L);
        when(roomInspectionRepository.countCompletedToday(any(), any(), any(), any())).thenReturn(1L);

        when(housekeepingTaskRepository.findCompletedToday(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(hk));
        when(maintenanceTicketRepository.findCompletedToday(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(mt));
        when(roomInspectionRepository.findCompletedToday(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(ri));

        when(housekeepingTaskRepository.findTodayForDashboard(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(hk));
        when(maintenanceTicketRepository.findTodayForDashboard(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(mt));
        when(roomInspectionRepository.findTodayForDashboard(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(ri));

        EmployeeDashboardResponse res = service.getDashboard(employee);
        assertFalse(res.isNoPropertyAssignment());
        assertEquals(1, res.getHousekeepingTasks().size());
        assertEquals(1, res.getMaintenanceTickets().size());
        assertEquals(1, res.getInspections().size());
        assertEquals(3, res.getTodayTasks().size());
        assertEquals(3, res.getAwaiting().getPreviews().size());
        assertEquals(3, res.getCompletedToday().getPreviews().size());
        assertEquals("HOUSEKEEPING", res.getTodayTasks().get(0).getType());
    }
}
