package com.homestay.service;

import com.homestay.dto.request.AdminUpdateUserRequest;
import com.homestay.dto.request.UpdateCustomerStatusRequest;
import com.homestay.dto.response.AdminUserResponse;
import com.homestay.entity.ManagerPropertyAssignment;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ConflictException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.PaymentRepository;
import com.homestay.repository.RefreshTokenRepository;
import com.homestay.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
class AdminUserServiceCustomerTest {

    @Mock UserRepository userRepository;
    @Mock ManagerPropertyAssignmentRepository managerAssignmentRepository;
    @Mock BookingRepository bookingRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock ActivityLogService activityLogService;
    @Mock PasswordEncoder passwordEncoder;

    private AdminUserService service;
    private User admin;
    private User customer;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(
                userRepository,
                managerAssignmentRepository,
                bookingRepository,
                paymentRepository,
                refreshTokenRepository,
                activityLogService,
                passwordEncoder);
        admin = user(User.Role.ADMIN);
        customer = user(User.Role.CUSTOMER);
        customer.setStatus(User.Status.ACTIVE);
    }

    private static User user(User.Role role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setFullName(role.name() + " User");
        u.setEmail(role.name().toLowerCase() + "@test.local");
        u.setRole(role);
        u.setStatus(User.Status.ACTIVE);
        return u;
    }

    @Test
    void suspendCustomer_revokesRefreshTokens_andLogs() {
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.countByCustomerId(customer.getId())).thenReturn(0L);
        when(paymentRepository.sumPaidAmountByCustomerId(customer.getId())).thenReturn(null);

        UpdateCustomerStatusRequest req = new UpdateCustomerStatusRequest();
        req.setStatus("SUSPENDED");

        AdminUserResponse res = service.updateCustomerStatus(customer.getId(), req, admin);

        assertEquals("SUSPENDED", res.getStatus());
        verify(refreshTokenRepository).revokeAllByUser(customer);
        verify(activityLogService).log(
                eq(admin),
                eq(ActivityLogService.USER_SUSPENDED),
                eq("User"),
                eq(customer.getId()),
                eq("ACTIVE → SUSPENDED"));
    }

    @Test
    void activateCustomer_logsWithoutRevoke() {
        customer.setStatus(User.Status.SUSPENDED);
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.countByCustomerId(customer.getId())).thenReturn(2L);
        when(paymentRepository.sumPaidAmountByCustomerId(customer.getId())).thenReturn(null);

        UpdateCustomerStatusRequest req = new UpdateCustomerStatusRequest();
        req.setStatus("ACTIVE");

        AdminUserResponse res = service.updateCustomerStatus(customer.getId(), req, admin);

        assertEquals("ACTIVE", res.getStatus());
        verify(refreshTokenRepository, never()).revokeAllByUser(any());
        verify(activityLogService).log(
                eq(admin),
                eq(ActivityLogService.USER_ACTIVATED),
                eq("User"),
                eq(customer.getId()),
                eq("SUSPENDED → ACTIVE"));
    }

    @Test
    void suspendInactiveCustomer_throwsConflict() {
        customer.setStatus(User.Status.INACTIVE);
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        UpdateCustomerStatusRequest req = new UpdateCustomerStatusRequest();
        req.setStatus("SUSPENDED");

        assertThrows(ConflictException.class,
                () -> service.updateCustomerStatus(customer.getId(), req, admin));
        verify(refreshTokenRepository, never()).revokeAllByUser(any());
        verify(activityLogService, never()).log(any(), any(), any(), any(), any());
    }

    @Test
    void updateUser_customerInactiveTarget_rejected() {
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setStatus("INACTIVE");

        assertThrows(BusinessException.class,
                () -> service.updateUser(customer.getId(), req, admin));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_managerActiveInactive_unchanged() {
        User manager = user(User.Role.MANAGER);
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(managerAssignmentRepository.countByManager_IdAndStatus(
                eq(manager.getId()), eq(ManagerPropertyAssignment.Status.ACTIVE)))
                .thenReturn(0L);

        AdminUpdateUserRequest req = new AdminUpdateUserRequest();
        req.setStatus("INACTIVE");

        AdminUserResponse res = service.updateUser(manager.getId(), req, admin);
        assertEquals("INACTIVE", res.getStatus());
        verify(refreshTokenRepository, never()).revokeAllByUser(any());
        verify(activityLogService, never()).log(any(), any(), any(), any(), any());
    }

    @Test
    void updateCustomerStatus_nonCustomer_throws() {
        User manager = user(User.Role.MANAGER);
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));

        UpdateCustomerStatusRequest req = new UpdateCustomerStatusRequest();
        req.setStatus("SUSPENDED");

        assertThrows(BusinessException.class,
                () -> service.updateCustomerStatus(manager.getId(), req, admin));
    }

    @Test
    void updateCustomerStatus_missingUser_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        UpdateCustomerStatusRequest req = new UpdateCustomerStatusRequest();
        req.setStatus("SUSPENDED");

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateCustomerStatus(id, req, admin));
    }
}
