package com.homestay.service;

import com.homestay.dto.response.CustomerDetailResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.RefreshTokenRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceManagerApiTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock BookingRepository bookingRepository;

    private UserService service;
    private User customer;
    private Property property;
    private Room room;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, refreshTokenRepository, passwordEncoder, bookingRepository);
        customer = TestFixtures.user(User.Role.CUSTOMER);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
    }

    @Test
    void getAllCustomers() {
        when(userRepository.findByRoleWithFilters(
                eq(User.Role.CUSTOMER), eq(User.Status.ACTIVE), eq("mai"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(customer)));
        when(bookingRepository.countByCustomerId(customer.getId())).thenReturn(2L);

        PageResponse<?> page = service.getAllCustomers(0, 10, "ACTIVE", "mai");
        assertEquals(1, page.getContent().size());
    }

    @Test
    void getAllCustomers_allStatus() {
        when(userRepository.findByRoleWithFilters(
                eq(User.Role.CUSTOMER), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(customer)));
        when(bookingRepository.countByCustomerId(customer.getId())).thenReturn(0L);
        assertEquals(1, service.getAllCustomers(0, 10, "ALL", " ").getContent().size());
    }

    @Test
    void getCustomerDetail_ok() {
        Booking booking = TestFixtures.booking(customer, room, Booking.Status.CONFIRMED);
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(bookingRepository.findByCustomerId(eq(customer.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(booking)));
        when(bookingRepository.countByCustomerId(customer.getId())).thenReturn(1L);

        CustomerDetailResponse detail = service.getCustomerDetail(customer.getId());
        assertEquals(customer.getId(), detail.getId());
        assertEquals(1, detail.getRecentBookings().size());
    }

    @Test
    void getCustomerDetail_rejectsNonCustomer() {
        User manager = TestFixtures.user(User.Role.MANAGER);
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        assertThrows(BusinessException.class, () -> service.getCustomerDetail(manager.getId()));
    }

    @Test
    void updateCustomerStatus() {
        when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.updateCustomerStatus(customer.getId(), "SUSPENDED");
        assertEquals(User.Status.SUSPENDED, customer.getStatus());
        verify(userRepository).save(customer);
    }

    @Test
    void updateCustomerStatus_rejectsNonCustomer() {
        User manager = TestFixtures.user(User.Role.MANAGER);
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));
        assertThrows(BusinessException.class, () ->
                service.updateCustomerStatus(manager.getId(), "ACTIVE"));
    }
}
