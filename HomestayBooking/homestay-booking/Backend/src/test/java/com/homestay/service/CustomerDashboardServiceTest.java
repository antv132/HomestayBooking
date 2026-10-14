package com.homestay.service;

import com.homestay.dto.response.CustomerDashboardResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.MaintenanceTicket;
import com.homestay.entity.Payment;
import com.homestay.entity.User;
import com.homestay.exception.ForbiddenException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.MaintenanceTicketRepository;
import com.homestay.repository.NotificationRepository;
import com.homestay.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
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
class CustomerDashboardServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock MaintenanceTicketRepository maintenanceTicketRepository;

    private CustomerDashboardService service;
    private User customer;

    @BeforeEach
    void setUp() {
        service = new CustomerDashboardService(
                bookingRepository,
                paymentRepository,
                notificationRepository,
                maintenanceTicketRepository);
        customer = new User();
        customer.setId(UUID.randomUUID());
        customer.setRole(User.Role.CUSTOMER);
        customer.setStatus(User.Status.ACTIVE);
        customer.setFullName("Nguyen Van A");
        customer.setEmail("a@test.local");
    }

    @Test
    void getDashboard_nonCustomer_forbidden() {
        User manager = new User();
        manager.setId(UUID.randomUUID());
        manager.setRole(User.Role.MANAGER);
        assertThrows(ForbiddenException.class, () -> service.getDashboard(manager));
    }

    @Test
    void getDashboard_activeExcludesPendingDeposit_andLimitsLists() {
        when(bookingRepository.countActiveByCustomerId(customer.getId())).thenReturn(2L);
        when(bookingRepository.countPendingDepositByCustomerId(customer.getId())).thenReturn(1L);
        when(paymentRepository.countByCustomerIdAndStatus(customer.getId(), Payment.Status.PENDING))
                .thenReturn(1L);
        when(maintenanceTicketRepository.countByCustomerIdAndStatusIn(
                eq(customer.getId()),
                eq(List.of(MaintenanceTicket.Status.OPEN, MaintenanceTicket.Status.IN_PROGRESS))))
                .thenReturn(3L);
        when(notificationRepository.countByUserIdAndIsReadFalse(customer.getId())).thenReturn(4L);
        when(bookingRepository.findFirstByCustomerIdAndStatusAndCheckInDateGreaterThanEqualOrderByCheckInDateAsc(
                eq(customer.getId()), eq(Booking.Status.CONFIRMED), any()))
                .thenReturn(Optional.empty());
        when(bookingRepository.findFirstByCustomerIdAndStatusAndCheckOutDateGreaterThanEqualOrderByCheckOutDateAsc(
                eq(customer.getId()), eq(Booking.Status.CHECKED_IN), any()))
                .thenReturn(Optional.empty());
        when(bookingRepository.findUpcomingByCustomerId(eq(customer.getId()), any(), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(paymentRepository.findByCustomerIdOrderByCreatedAtDesc(eq(customer.getId()), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(customer.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        CustomerDashboardResponse res = service.getDashboard(customer);

        assertEquals(2, res.getActiveBookings());
        assertEquals(2, res.getPendingPayments()); // 1 pending deposit + 1 payment PENDING
        assertEquals(3, res.getOpenTickets());
        assertEquals(4, res.getUnreadNotifications());

        ArgumentCaptor<Pageable> upcomingPage = ArgumentCaptor.forClass(Pageable.class);
        verify(bookingRepository).findUpcomingByCustomerId(eq(customer.getId()), any(), upcomingPage.capture());
        assertEquals(5, upcomingPage.getValue().getPageSize());

        ArgumentCaptor<Pageable> paymentPage = ArgumentCaptor.forClass(Pageable.class);
        verify(paymentRepository).findByCustomerIdOrderByCreatedAtDesc(eq(customer.getId()), paymentPage.capture());
        assertEquals(3, paymentPage.getValue().getPageSize());
    }
}
