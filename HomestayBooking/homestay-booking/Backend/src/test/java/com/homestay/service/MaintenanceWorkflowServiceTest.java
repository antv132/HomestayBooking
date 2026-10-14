package com.homestay.service;

import com.homestay.dto.request.UpdateMaintenanceStatusRequest;
import com.homestay.entity.Booking;
import com.homestay.entity.MaintenanceTicket;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.repository.EmployeePropertyAssignmentRepository;
import com.homestay.repository.MaintenanceTicketRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceWorkflowServiceTest {

    @Mock MaintenanceTicketRepository ticketRepository;
    @Mock EmployeePropertyAssignmentRepository employeeAssignmentRepository;
    @Mock ManagerPropertyAssignmentRepository managerAssignmentRepository;
    @Mock NotificationService notificationService;
    @Mock ActivityLogService activityLogService;

    private MaintenanceTaskManagerService managerService;
    private EmployeeMaintenanceService employeeService;

    private User manager;
    private User employee;
    private User customer;
    private Property property;
    private Room room;
    private Booking booking;
    private MaintenanceTicket ticket;

    @BeforeEach
    void setUp() {
        ReportPropertyScopeValidator scopeValidator =
                new ReportPropertyScopeValidator(managerAssignmentRepository);
        managerService = new MaintenanceTaskManagerService(
                ticketRepository,
                employeeAssignmentRepository,
                scopeValidator,
                notificationService,
                activityLogService);
        employeeService = new EmployeeMaintenanceService(
                ticketRepository,
                notificationService,
                activityLogService);

        manager = TestFixtures.user(User.Role.MANAGER);
        employee = TestFixtures.user(User.Role.EMPLOYEE);
        customer = TestFixtures.user(User.Role.CUSTOMER);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
        booking = TestFixtures.booking(customer, room, Booking.Status.CHECKED_IN);
        ticket = new MaintenanceTicket();
        ticket.setId(UUID.randomUUID());
        ticket.setCustomer(customer);
        ticket.setRoom(room);
        ticket.setBooking(booking);
        ticket.setTitle("Broken AC unit");
        ticket.setDescription("Air conditioner not cooling the room properly.");
        ticket.setStatus(MaintenanceTicket.Status.OPEN);
    }

    private void stubManagerScopeOk() {
        when(managerAssignmentRepository.existsByManagerIdAndPropertyIdAndStatus(
                eq(manager.getId()), eq(property.getId()), any()))
                .thenReturn(true);
    }

    @Test
    void managerClose_requiresNoteMin10() {
        ticket.setStatus(MaintenanceTicket.Status.RESOLVED);
        when(ticketRepository.findByIdWithDetails(ticket.getId())).thenReturn(Optional.of(ticket));
        stubManagerScopeOk();

        UpdateMaintenanceStatusRequest req = new UpdateMaintenanceStatusRequest();
        req.setStatus(MaintenanceTicket.Status.CLOSED);
        req.setResolutionNote("short");

        assertThrows(BusinessException.class,
                () -> managerService.updateStatusForManager(manager, ticket.getId(), req));
    }

    @Test
    void managerClose_fromResolved_ok() {
        ticket.setStatus(MaintenanceTicket.Status.RESOLVED);
        when(ticketRepository.findByIdWithDetails(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(MaintenanceTicket.class))).thenAnswer(inv -> inv.getArgument(0));
        stubManagerScopeOk();

        UpdateMaintenanceStatusRequest req = new UpdateMaintenanceStatusRequest();
        req.setStatus(MaintenanceTicket.Status.CLOSED);
        req.setResolutionNote("Verified repair completed successfully.");

        managerService.updateStatusForManager(manager, ticket.getId(), req);

        assertEquals(MaintenanceTicket.Status.CLOSED, ticket.getStatus());
        verify(activityLogService).log(eq(manager), eq(ActivityLogService.TICKET_CLOSED),
                eq("MaintenanceTicket"), eq(ticket.getId()), any());
    }

    @Test
    void managerClose_whenAlreadyClosed_blocked() {
        ticket.setStatus(MaintenanceTicket.Status.CLOSED);
        when(ticketRepository.findByIdWithDetails(ticket.getId())).thenReturn(Optional.of(ticket));
        stubManagerScopeOk();

        UpdateMaintenanceStatusRequest req = new UpdateMaintenanceStatusRequest();
        req.setStatus(MaintenanceTicket.Status.CLOSED);
        req.setResolutionNote("Trying again after close.");

        assertThrows(BusinessException.class,
                () -> managerService.updateStatusForManager(manager, ticket.getId(), req));
    }

    @Test
    void employeeResolve_storesWorkNote_notResolutionNote() {
        ticket.setStatus(MaintenanceTicket.Status.IN_PROGRESS);
        ticket.setAssignedEmployee(employee);
        when(ticketRepository.findByIdAndAssignedEmployeeId(ticket.getId(), employee.getId()))
                .thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(MaintenanceTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateMaintenanceStatusRequest req = new UpdateMaintenanceStatusRequest();
        req.setStatus(MaintenanceTicket.Status.RESOLVED);
        req.setResolutionNote("Replaced filter and tested cooling.");

        employeeService.updateStatus(employee, ticket.getId(), req);

        assertEquals(MaintenanceTicket.Status.RESOLVED, ticket.getStatus());
        assertEquals("Replaced filter and tested cooling.", ticket.getWorkNote());
        assertEquals(null, ticket.getResolutionNote());
        verify(notificationService).sendNotification(
                eq(customer.getId()), any(), any(), any(), eq(ticket.getId()), eq("MaintenanceTicket"));
    }

    @Test
    void employeeCannotSkipToResolvedFromAssigned() {
        ticket.setStatus(MaintenanceTicket.Status.ASSIGNED);
        ticket.setAssignedEmployee(employee);
        when(ticketRepository.findByIdAndAssignedEmployeeId(ticket.getId(), employee.getId()))
                .thenReturn(Optional.of(ticket));

        UpdateMaintenanceStatusRequest req = new UpdateMaintenanceStatusRequest();
        req.setStatus(MaintenanceTicket.Status.RESOLVED);
        req.setResolutionNote("Done somehow");

        assertThrows(BusinessException.class,
                () -> employeeService.updateStatus(employee, ticket.getId(), req));
    }
}
