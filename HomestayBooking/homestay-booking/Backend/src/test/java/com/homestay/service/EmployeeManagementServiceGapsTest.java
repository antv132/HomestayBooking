package com.homestay.service;

import com.homestay.dto.request.CreateEmployeeRequest;
import com.homestay.dto.request.UpdateEmployeeRequest;
import com.homestay.dto.response.PageResponse;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

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
class EmployeeManagementServiceGapsTest {

    @Mock EmployeePropertyAssignmentRepository assignmentRepository;
    @Mock UserRepository userRepository;
    @Mock PropertyRepository propertyRepository;
    @Mock ReportPropertyScopeValidator scopeValidator;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ActivityLogService activityLogService;
    @Mock EmailService emailService;

    private EmployeeManagementService service;
    private User manager;
    private User admin;
    private Property property;
    private User employee;
    private EmployeePropertyAssignment assignment;

    @BeforeEach
    void setUp() {
        service = new EmployeeManagementService(
                assignmentRepository, userRepository, propertyRepository,
                scopeValidator, passwordEncoder, activityLogService, emailService);
        manager = TestFixtures.user(User.Role.MANAGER);
        admin = TestFixtures.user(User.Role.ADMIN);
        property = TestFixtures.property();
        property.setStatus(Property.Status.ACTIVE);
        employee = TestFixtures.user(User.Role.EMPLOYEE);
        assignment = new EmployeePropertyAssignment();
        assignment.setId(UUID.randomUUID());
        assignment.setEmployee(employee);
        assignment.setProperty(property);
        assignment.setStatus(EmployeePropertyAssignment.Status.ACTIVE);
    }

    @Test
    void listByProperty() {
        when(assignmentRepository.findByPropertyIdAndStatusWithSearch(
                eq(property.getId()), eq(EmployeePropertyAssignment.Status.ACTIVE), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(assignment)));
        PageResponse<?> page = service.listByProperty(manager, property.getId(), "  ", 0, 10);
        assertEquals(1, page.getContent().size());
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
    }

    @Test
    void listAdmin_requiresAdmin() {
        assertThrows(ForbiddenException.class, () ->
                service.listAdmin(manager, null, null, 0, 10));
    }

    @Test
    void listAdmin_ok() {
        when(assignmentRepository.findActiveWithOptionalPropertyAndSearch(
                eq(null), eq(EmployeePropertyAssignment.Status.ACTIVE), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(assignment)));
        assertEquals(1, service.listAdmin(admin, null, null, 0, 10).getContent().size());
    }

    @Test
    void listUnassigned() {
        when(assignmentRepository.findUnassignedEmployees(
                eq("an"),
                eq(User.Role.EMPLOYEE),
                eq(User.Status.ACTIVE),
                eq(EmployeePropertyAssignment.Status.ACTIVE),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(employee)));
        assertEquals(1, service.listUnassigned(manager, property.getId(), "an", 0, 10).getContent().size());
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
    }

    @Test
    void createEmployee_duplicateEmail() {
        CreateEmployeeRequest req = new CreateEmployeeRequest();
        req.setFullName("Emp Two");
        req.setEmail("Dup@Test.Local");
        req.setPropertyId(property.getId());
        when(userRepository.existsByEmail("dup@test.local")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.createEmployee(manager, req));
    }

    @Test
    void updateEmployee_ok() {
        when(assignmentRepository.findByEmployee_IdAndProperty_IdAndStatus(
                employee.getId(), property.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(Optional.of(assignment));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateEmployeeRequest req = new UpdateEmployeeRequest();
        req.setFullName("Updated Emp Name");
        req.setPhone("0901234567");
        assertEquals("Updated Emp Name",
                service.updateEmployee(manager, employee.getId(), property.getId(), req).getFullName());
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
    }
}
