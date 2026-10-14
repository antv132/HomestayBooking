package com.homestay.service;

import com.homestay.dto.request.DisputeDamageReportRequest;
import com.homestay.dto.response.CustomerDamageReportResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.DamageReport;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.RoomInspection;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ForbiddenException;
import com.homestay.repository.DamageReportRepository;
import com.homestay.repository.UserRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerDamageReportServiceTest {

    @Mock DamageReportRepository damageReportRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;

    @InjectMocks CustomerDamageReportService service;

    private User customer;
    private User otherCustomer;
    private User admin;
    private User inspector;
    private Property property;
    private Room room;
    private Booking booking;
    private RoomInspection inspection;

    @BeforeEach
    void setUp() {
        customer = TestFixtures.user(User.Role.CUSTOMER);
        otherCustomer = TestFixtures.user(User.Role.CUSTOMER);
        admin = TestFixtures.user(User.Role.ADMIN);
        inspector = TestFixtures.user(User.Role.EMPLOYEE);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
        booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_DAMAGE_PAYMENT);
        inspection = TestFixtures.inspection(booking, property, room);
        inspection.setInspectedBy(inspector);
    }

    @Test
    void dispute_within24h_setsDisputedAndNotifiesAdmin() {
        DamageReport dr = approvedReport(LocalDateTime.now().minusHours(2));
        when(damageReportRepository.findDetailById(dr.getId())).thenReturn(Optional.of(dr));
        when(damageReportRepository.save(any(DamageReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByRole(User.Role.ADMIN)).thenReturn(List.of(admin));

        DisputeDamageReportRequest req = new DisputeDamageReportRequest();
        req.setNote("Tôi không đồng ý với mức phí bồi thường này.");

        CustomerDamageReportResponse resp = service.dispute(customer, dr.getId(), req);

        assertEquals(DamageReport.Status.DISPUTED.name(), resp.getStatus());
        assertEquals(DamageReport.Status.DISPUTED, dr.getStatus());
        assertEquals(req.getNote(), dr.getDisputeNote());
        assertNotNull(dr.getDisputedAt());
        assertFalse(resp.isDisputeEligible());
        verify(notificationService, atLeastOnce()).sendNotification(
                eq(admin.getId()), any(), any(), any(), eq(dr.getId()), eq("DamageReport"));
    }

    @Test
    void dispute_after24h_rejected() {
        DamageReport dr = approvedReport(LocalDateTime.now().minusHours(25));
        when(damageReportRepository.findDetailById(dr.getId())).thenReturn(Optional.of(dr));

        DisputeDamageReportRequest req = new DisputeDamageReportRequest();
        req.setNote("Tôi không đồng ý với mức phí bồi thường này.");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.dispute(customer, dr.getId(), req));
        assertTrue(ex.getMessage().contains("DISPUTE_WINDOW_EXPIRED"));
    }

    @Test
    void dispute_nonOwner_forbidden() {
        DamageReport dr = approvedReport(LocalDateTime.now().minusHours(1));
        when(damageReportRepository.findDetailById(dr.getId())).thenReturn(Optional.of(dr));

        DisputeDamageReportRequest req = new DisputeDamageReportRequest();
        req.setNote("Tôi không đồng ý với mức phí bồi thường này.");

        assertThrows(ForbiddenException.class,
                () -> service.dispute(otherCustomer, dr.getId(), req));
    }

    @Test
    void list_returnsCustomerReports() {
        DamageReport dr = approvedReport(LocalDateTime.now());
        when(damageReportRepository.findForCustomer(customer.getId())).thenReturn(List.of(dr));

        List<CustomerDamageReportResponse> list = service.listForCustomer(customer);

        assertEquals(1, list.size());
        assertTrue(list.get(0).isDisputeEligible());
        assertNotNull(list.get(0).getDisputeDeadline());
    }

    private DamageReport approvedReport(LocalDateTime approvedAt) {
        DamageReport dr = TestFixtures.damageReport(
                booking, inspection, new BigDecimal("1500000"), false);
        dr.setStatus(DamageReport.Status.APPROVED);
        dr.setApprovedAmount(new BigDecimal("1500000"));
        dr.setApprovedAt(approvedAt);
        return dr;
    }
}
