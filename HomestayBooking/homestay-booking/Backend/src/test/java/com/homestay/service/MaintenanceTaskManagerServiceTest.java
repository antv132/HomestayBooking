package com.homestay.service;

import com.homestay.dto.request.AssignMaintenanceTaskRequest;
import com.homestay.dto.request.UpdateMaintenanceStatusRequest;
import com.homestay.dto.response.MaintenanceTaskSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.EmployeePropertyAssignment;
import com.homestay.entity.MaintenanceTicket;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.repository.EmployeePropertyAssignmentRepository;
import com.homestay.repository.MaintenanceTicketRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceTaskManagerServiceTest {

    @Mock MaintenanceTicketRepository ticketRepository;
    @Mock EmployeePropertyAssignmentRepository employeeAssignmentRepository;
    @Mock ReportPropertyScopeValidator scopeValidator;
    @Mock NotificationService notificationService;
    @Mock ActivityLogService activityLogService;

    private MaintenanceTaskManagerService service;
    private User manager;
    private User employee;
    private User customer;
    private Property property;
    private Room room;
    private MaintenanceTicket ticket;

    @BeforeEach
    void setUp() {
        service = new MaintenanceTaskManagerService(
                ticketRepository,
                employeeAssignmentRepository,
                scopeValidator,
                notificationService,
                activityLogService);
        manager = TestFixtures.user(User.Role.MANAGER);
        employee = TestFixtures.user(User.Role.EMPLOYEE);
        customer = TestFixtures.user(User.Role.CUSTOMER);
        property = TestFixtures.property();
        room = TestFixtures.room(property);

        ticket = new MaintenanceTicket();
        ticket.setId(UUID.randomUUID());
        ticket.setRoom(room);
        ticket.setCustomer(customer);
        ticket.setTitle("Leaky faucet");
        ticket.setDescription("Kitchen sink leaking water onto floor.");
        ticket.setStatus(MaintenanceTicket.Status.OPEN);
    }

    @Test
    void listForManager() {
        when(ticketRepository.findForManagerBoard(
                eq(property.getId()), eq(null), eq("leak"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket)));

        PageResponse<MaintenanceTaskSummaryResponse> page =
                service.listForManager(manager, property.getId(), null, " leak ", 0, 10);
        assertEquals(1, page.getContent().size());
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
    }

    @Test
    void assignForManager_openToAssigned() {
        when(ticketRepository.findByIdWithDetails(ticket.getId())).thenReturn(Optional.of(ticket));
        when(employeeAssignmentRepository.existsByEmployeeIdAndPropertyIdAndStatus(
                employee.getId(), property.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(true);
        when(employeeAssignmentRepository.findActiveEmployeesByPropertyId(
                property.getId(), EmployeePropertyAssignment.Status.ACTIVE, User.Role.EMPLOYEE))
                .thenReturn(List.of(employee));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssignMaintenanceTaskRequest req = new AssignMaintenanceTaskRequest();
        req.setAssigneeId(employee.getId());
        MaintenanceTaskSummaryResponse res = service.assignForManager(manager, ticket.getId(), req);
        assertEquals(MaintenanceTicket.Status.ASSIGNED.name(), res.getStatus());
        verify(activityLogService).log(eq(manager), eq(ActivityLogService.TICKET_ASSIGNED),
                eq("MaintenanceTicket"), eq(ticket.getId()), any());
    }

    @Test
    void assignForManager_rejectsClosed() {
        ticket.setStatus(MaintenanceTicket.Status.CLOSED);
        when(ticketRepository.findByIdWithDetails(ticket.getId())).thenReturn(Optional.of(ticket));
        AssignMaintenanceTaskRequest req = new AssignMaintenanceTaskRequest();
        req.setAssigneeId(employee.getId());
        assertThrows(BusinessException.class, () ->
                service.assignForManager(manager, ticket.getId(), req));
    }

    @Test
    void updateStatusForManager_closesResolved() {
        ticket.setStatus(MaintenanceTicket.Status.RESOLVED);
        when(ticketRepository.findByIdWithDetails(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateMaintenanceStatusRequest req = new UpdateMaintenanceStatusRequest();
        req.setStatus(MaintenanceTicket.Status.CLOSED);
        req.setResolutionNote("Verified fix and customer confirmed.");
        MaintenanceTaskSummaryResponse res = service.updateStatusForManager(manager, ticket.getId(), req);
        assertEquals(MaintenanceTicket.Status.CLOSED.name(), res.getStatus());
        verify(activityLogService).log(eq(manager), eq(ActivityLogService.TICKET_CLOSED),
                eq("MaintenanceTicket"), eq(ticket.getId()), any());
    }

    @Test
    void updateStatusForManager_rejectsShortNote() {
        ticket.setStatus(MaintenanceTicket.Status.RESOLVED);
        when(ticketRepository.findByIdWithDetails(ticket.getId())).thenReturn(Optional.of(ticket));
        UpdateMaintenanceStatusRequest req = new UpdateMaintenanceStatusRequest();
        req.setStatus(MaintenanceTicket.Status.CLOSED);
        req.setResolutionNote("short");
        assertThrows(BusinessException.class, () ->
                service.updateStatusForManager(manager, ticket.getId(), req));
    }

    @Test
    void updateStatusForManager_rejectsNonClosedTarget() {
        ticket.setStatus(MaintenanceTicket.Status.RESOLVED);
        when(ticketRepository.findByIdWithDetails(ticket.getId())).thenReturn(Optional.of(ticket));
        UpdateMaintenanceStatusRequest req = new UpdateMaintenanceStatusRequest();
        req.setStatus(MaintenanceTicket.Status.IN_PROGRESS);
        assertThrows(BusinessException.class, () ->
                service.updateStatusForManager(manager, ticket.getId(), req));
    }
}
