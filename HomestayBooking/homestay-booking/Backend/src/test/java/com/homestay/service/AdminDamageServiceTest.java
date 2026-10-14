package com.homestay.service;

import com.homestay.entity.Booking;
import com.homestay.entity.DamageReport;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.RoomInspection;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.repository.AttachmentRepository;
import com.homestay.repository.DamageReportRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDamageServiceTest {

    @Mock DamageReportRepository damageReportRepository;
    @Mock AttachmentRepository attachmentRepository;
    @Mock NotificationService notificationService;
    @Mock DamageFeeSettlementService damageFeeSettlementService;

    @InjectMocks AdminDamageService service;

    private User admin;
    private User manager;
    private User customer;
    private Property property;
    private Room room;
    private Booking booking;
    private RoomInspection inspection;

    @BeforeEach
    void setUp() {
        admin = TestFixtures.user(User.Role.ADMIN);
        manager = TestFixtures.user(User.Role.MANAGER);
        customer = TestFixtures.user(User.Role.CUSTOMER);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
        booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_INSPECTION);
        inspection = TestFixtures.inspection(booking, property, room);
    }

    @Test
    void coApprove_withoutManagerEscalate_throws() {
        DamageReport dr = TestFixtures.damageReport(
                booking, inspection, new BigDecimal("6000000"), true);
        when(damageReportRepository.findDetailById(dr.getId())).thenReturn(Optional.of(dr));

        assertThrows(BusinessException.class,
                () -> service.coApprove(dr.getId(), new BigDecimal("6000000"), admin));
        verify(damageFeeSettlementService, never()).applyApprovedFee(any(), any());
        verify(damageReportRepository, never()).save(any());
    }

    @Test
    void coApprove_afterManagerEscalate_approvesAndSettles() {
        DamageReport dr = TestFixtures.damageReport(
                booking, inspection, new BigDecimal("6000000"), true);
        dr.setApprovedBy(manager);
        when(damageReportRepository.findDetailById(dr.getId())).thenReturn(Optional.of(dr));
        when(damageReportRepository.save(any(DamageReport.class))).thenAnswer(inv -> inv.getArgument(0));
        var resp = service.coApprove(dr.getId(), new BigDecimal("5500000"), admin);

        assertEquals("APPROVED", resp.getStatus());
        assertEquals(DamageReport.Status.APPROVED, dr.getStatus());
        assertEquals(admin, dr.getAdminApprover());
        verify(damageFeeSettlementService).applyApprovedFee(eq(dr), eq(new BigDecimal("5500000")));
    }
}
