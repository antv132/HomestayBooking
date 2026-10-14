package com.homestay.service;

import com.homestay.dto.request.AdminCreateManagerRequest;
import com.homestay.dto.request.AdminUpdateManagerRequest;
import com.homestay.dto.response.AdminUserResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.ManagerPropertyAssignment;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.PaymentRepository;
import com.homestay.repository.RefreshTokenRepository;
import com.homestay.repository.UserRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
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
class AdminUserServiceDirectoryTest {

    @Mock UserRepository userRepository;
    @Mock ManagerPropertyAssignmentRepository managerAssignmentRepository;
    @Mock BookingRepository bookingRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock ActivityLogService activityLogService;
    @Mock PasswordEncoder passwordEncoder;

    private AdminUserService service;

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
    }

    @Test
    void createManager_success() {
        AdminCreateManagerRequest req = new AdminCreateManagerRequest();
        req.setFullName("  New Manager ");
        req.setEmail(" Mgr@Test.Local ");
        req.setPhone("090");
        req.setPassword("password1");
        when(userRepository.existsByEmail("mgr@test.local")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        AdminUserResponse res = service.createManager(req);
        assertEquals(User.Role.MANAGER.name(), res.getRole());
        assertEquals(User.Status.ACTIVE.name(), res.getStatus());
        assertEquals("mgr@test.local", res.getEmail());
    }

    @Test
    void createManager_duplicateEmail() {
        AdminCreateManagerRequest req = new AdminCreateManagerRequest();
        req.setFullName("X");
        req.setEmail("dup@test.local");
        req.setPassword("password1");
        when(userRepository.existsByEmail("dup@test.local")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.createManager(req));
    }

    @Test
    void updateManager_rejectsNonManager() {
        User customer = TestFixtures.user(User.Role.CUSTOMER);
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        AdminUpdateManagerRequest req = new AdminUpdateManagerRequest();
        req.setFullName("A");
        req.setEmail("a@test.local");
        assertThrows(BusinessException.class, () -> service.updateManager(customer.getId(), req));
    }

    @Test
    void updateManager_passwordTooShort() {
        User manager = TestFixtures.user(User.Role.MANAGER);
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        AdminUpdateManagerRequest req = new AdminUpdateManagerRequest();
        req.setFullName("A");
        req.setEmail(manager.getEmail());
        req.setPassword("short");
        assertThrows(BusinessException.class, () -> service.updateManager(manager.getId(), req));
    }

    @Test
    void updateManager_setsInactive() {
        User manager = TestFixtures.user(User.Role.MANAGER);
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(managerAssignmentRepository.countByManager_IdAndStatus(
                manager.getId(), ManagerPropertyAssignment.Status.ACTIVE)).thenReturn(2L);

        AdminUpdateManagerRequest req = new AdminUpdateManagerRequest();
        req.setFullName("Mgr");
        req.setEmail(manager.getEmail());
        req.setStatus("INACTIVE");

        AdminUserResponse res = service.updateManager(manager.getId(), req);
        assertEquals(User.Status.INACTIVE.name(), res.getStatus());
        assertEquals(2, res.getPropertiesAssigned());
    }

    @Test
    void listUsers_rejectsAdminRole() {
        assertThrows(BusinessException.class, () ->
                service.listUsers("ADMIN", null, null, PageRequest.of(0, 10)));
    }

    @Test
    void listUsers_managersWithCounts() {
        User manager = TestFixtures.user(User.Role.MANAGER);
        when(userRepository.findByRoleWithFilters(
                eq(User.Role.MANAGER), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(manager)));
        when(managerAssignmentRepository.countActiveAssignmentsByManagerIds(
                any(), eq(ManagerPropertyAssignment.Status.ACTIVE)))
                .thenReturn(List.<Object[]>of(new Object[]{manager.getId(), 3}));

        PageResponse<AdminUserResponse> page =
                service.listUsers("MANAGER", null, "  ", PageRequest.of(0, 10));
        assertEquals(1, page.getContent().size());
        assertEquals(3, page.getContent().get(0).getPropertiesAssigned());
    }

    @Test
    void listUsers_customersWithSpend() {
        User customer = TestFixtures.user(User.Role.CUSTOMER);
        when(userRepository.findByRoleWithFilters(
                eq(User.Role.CUSTOMER), eq(User.Status.ACTIVE), eq("mai"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(customer)));
        when(bookingRepository.countBookingsByCustomerIds(any()))
                .thenReturn(List.<Object[]>of(new Object[]{customer.getId(), 4L}));
        when(paymentRepository.sumPaidAmountByCustomerIds(any()))
                .thenReturn(List.<Object[]>of(new Object[]{customer.getId(), new BigDecimal("1000")}));

        PageResponse<AdminUserResponse> page =
                service.listUsers("CUSTOMER", "ACTIVE", "mai", PageRequest.of(0, 10));
        assertEquals(4L, page.getContent().get(0).getTotalBookings());
    }

    @Test
    void getById_rejectsAdmin() {
        User admin = TestFixtures.user(User.Role.ADMIN);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        assertThrows(BusinessException.class, () -> service.getById(admin.getId()));
    }

    @Test
    void getById_manager() {
        User manager = TestFixtures.user(User.Role.MANAGER);
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        when(managerAssignmentRepository.countByManager_IdAndStatus(
                manager.getId(), ManagerPropertyAssignment.Status.ACTIVE)).thenReturn(1L);
        AdminUserResponse res = service.getById(manager.getId());
        assertEquals(1, res.getPropertiesAssigned());
    }

    @Test
    void listCustomerBookings_rejectsNonCustomer() {
        User manager = TestFixtures.user(User.Role.MANAGER);
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        assertThrows(BusinessException.class, () ->
                service.listCustomerBookings(manager.getId(), 0, 10));
    }

    @Test
    void listCustomerBookings_ok() {
        User customer = TestFixtures.user(User.Role.CUSTOMER);
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(bookingRepository.findByCustomerIdWithDetails(eq(customer.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<?> page = service.listCustomerBookings(customer.getId(), -1, 100);
        assertEquals(0, page.getContent().size());
    }

    @Test
    void updateManager_notFound() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        AdminUpdateManagerRequest req = new AdminUpdateManagerRequest();
        req.setFullName("A");
        req.setEmail("a@b.c");
        assertThrows(ResourceNotFoundException.class, () ->
                service.updateManager(UUID.randomUUID(), req));
    }
}
