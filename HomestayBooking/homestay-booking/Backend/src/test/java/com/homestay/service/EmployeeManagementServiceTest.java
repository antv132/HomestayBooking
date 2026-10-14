package com.homestay.service;

import com.homestay.dto.request.AssignEmployeeRequest;
import com.homestay.dto.request.CreateEmployeeRequest;
import com.homestay.dto.request.ReassignEmployeeRequest;
import com.homestay.dto.request.UpdateEmployeeStatusRequest;
import com.homestay.dto.response.EmployeeSummaryResponse;
import com.homestay.entity.EmployeePropertyAssignment;
import com.homestay.entity.Property;
import com.homestay.entity.User;
import com.homestay.exception.ConflictException;
import com.homestay.exception.ForbiddenException;
import com.homestay.repository.EmployeePropertyAssignmentRepository;
import com.homestay.repository.PropertyRepository;
import com.homestay.repository.UserRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeManagementServiceTest {

    @Mock EmployeePropertyAssignmentRepository assignmentRepository;
    @Mock UserRepository userRepository;
    @Mock PropertyRepository propertyRepository;
    @Mock ReportPropertyScopeValidator scopeValidator;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ActivityLogService activityLogService;
    @Mock EmailService emailService;

    @InjectMocks EmployeeManagementService service;

    private User manager;
    private User admin;
    private Property property;
    private Property otherProperty;
    private User employee;

    @BeforeEach
    void setUp() {
        manager = TestFixtures.user(User.Role.MANAGER);
        admin = TestFixtures.user(User.Role.ADMIN);
        property = TestFixtures.property();
        property.setStatus(Property.Status.ACTIVE);
        otherProperty = TestFixtures.property();
        otherProperty.setId(UUID.randomUUID());
        otherProperty.setName("Other Homestay");
        otherProperty.setStatus(Property.Status.ACTIVE);
        employee = TestFixtures.user(User.Role.EMPLOYEE);
    }

    @Test
    void assignEmployee_rejectsWhenAlreadyActiveElsewhere() {
        AssignEmployeeRequest req = new AssignEmployeeRequest();
        req.setEmployeeId(employee.getId());
        req.setPropertyId(property.getId());

        doNothing().when(scopeValidator).validateManagerAccess(manager, property.getId());
        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(userRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(assignmentRepository.existsByEmployeeIdAndPropertyIdAndStatus(
                employee.getId(), property.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(false);

        EmployeePropertyAssignment existing = new EmployeePropertyAssignment();
        existing.setEmployee(employee);
        existing.setProperty(otherProperty);
        existing.setStatus(EmployeePropertyAssignment.Status.ACTIVE);
        when(assignmentRepository.findFirstByEmployee_IdAndStatus(
                employee.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () -> service.assignEmployee(manager, req));
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void assignEmployee_rejectsSuspendedEmployee() {
        employee.setStatus(User.Status.SUSPENDED);
        AssignEmployeeRequest req = new AssignEmployeeRequest();
        req.setEmployeeId(employee.getId());
        req.setPropertyId(property.getId());

        doNothing().when(scopeValidator).validateManagerAccess(manager, property.getId());
        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(userRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        assertThrows(com.homestay.exception.BusinessException.class,
                () -> service.assignEmployee(manager, req));
    }

    @Test
    void listByProperty_deniesManagerWithoutScope() {
        doThrow(new ForbiddenException("denied"))
                .when(scopeValidator).validateManagerAccess(manager, property.getId());

        assertThrows(ForbiddenException.class,
                () -> service.listByProperty(manager, property.getId(), null, 0, 20));
    }

    @Test
    void createEmployee_sendsInviteAndLogs() {
        CreateEmployeeRequest req = new CreateEmployeeRequest();
        req.setFullName("Nguyen Van A");
        req.setEmail("emp@test.local");
        req.setPropertyId(property.getId());

        doNothing().when(scopeValidator).validateManagerAccess(manager, property.getId());
        when(userRepository.existsByEmail("emp@test.local")).thenReturn(false);
        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(assignmentRepository.save(any(EmployeePropertyAssignment.class))).thenAnswer(inv -> {
            EmployeePropertyAssignment epa = inv.getArgument(0);
            epa.setId(UUID.randomUUID());
            return epa;
        });
        when(emailService.trySendPlainText(any(), any(), any())).thenReturn(true);

        EmployeeSummaryResponse res = service.createEmployee(manager, req);

        assertEquals("emp@test.local", res.getEmail());
        verify(emailService).trySendPlainText(eq("emp@test.local"), any(), any());
        verify(activityLogService).log(eq(manager), eq(ActivityLogService.EMPLOYEE_CREATED),
                eq("User"), any(), any());
    }

    @Test
    void reassignEmployee_inactivatesOldAndCreatesNew() {
        EmployeePropertyAssignment current = new EmployeePropertyAssignment();
        current.setId(UUID.randomUUID());
        current.setEmployee(employee);
        current.setProperty(property);
        current.setStatus(EmployeePropertyAssignment.Status.ACTIVE);

        ReassignEmployeeRequest req = new ReassignEmployeeRequest();
        req.setPropertyId(otherProperty.getId());

        when(propertyRepository.findById(otherProperty.getId())).thenReturn(Optional.of(otherProperty));
        when(assignmentRepository.findFirstByEmployee_IdAndStatus(
                employee.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(Optional.of(current));
        when(assignmentRepository.saveAndFlush(any(EmployeePropertyAssignment.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(assignmentRepository.save(any(EmployeePropertyAssignment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        EmployeeSummaryResponse res = service.reassignEmployee(admin, employee.getId(), req);

        assertEquals(otherProperty.getId(), res.getPropertyId());
        assertEquals(EmployeePropertyAssignment.Status.INACTIVE, current.getStatus());

        verify(assignmentRepository).saveAndFlush(current);
        ArgumentCaptor<EmployeePropertyAssignment> captor =
                ArgumentCaptor.forClass(EmployeePropertyAssignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertEquals(EmployeePropertyAssignment.Status.ACTIVE, captor.getValue().getStatus());
        assertEquals(otherProperty.getId(), captor.getValue().getProperty().getId());
        verify(activityLogService).log(eq(admin), eq(ActivityLogService.EMPLOYEE_REASSIGNED),
                eq("User"), eq(employee.getId()), any());
    }

    @Test
    void reassignEmployee_rejectsNonAdmin() {
        ReassignEmployeeRequest req = new ReassignEmployeeRequest();
        req.setPropertyId(otherProperty.getId());

        assertThrows(ForbiddenException.class,
                () -> service.reassignEmployee(manager, employee.getId(), req));
    }

    @Test
    void updateEmployeeStatus_logsChange() {
        EmployeePropertyAssignment epa = new EmployeePropertyAssignment();
        epa.setEmployee(employee);
        epa.setProperty(property);
        epa.setStatus(EmployeePropertyAssignment.Status.ACTIVE);

        UpdateEmployeeStatusRequest req = new UpdateEmployeeStatusRequest();
        req.setStatus(User.Status.SUSPENDED);

        doNothing().when(scopeValidator).validateManagerAccess(manager, property.getId());
        when(assignmentRepository.findByEmployee_IdAndProperty_IdAndStatus(
                employee.getId(), property.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(Optional.of(epa));
        when(userRepository.save(employee)).thenReturn(employee);

        service.updateEmployeeStatus(manager, employee.getId(), property.getId(), req);

        assertEquals(User.Status.SUSPENDED, employee.getStatus());
        verify(activityLogService).log(eq(manager), eq(ActivityLogService.EMPLOYEE_STATUS_CHANGED),
                eq("User"), eq(employee.getId()), any());
    }
}
