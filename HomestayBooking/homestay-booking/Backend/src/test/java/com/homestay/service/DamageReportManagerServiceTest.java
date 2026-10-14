package com.homestay.service;

import com.homestay.dto.request.ApproveDamageReportRequest;
import com.homestay.dto.response.DamageReportDetailResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.DamageReport;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.RoomInspection;
import com.homestay.entity.User;
import com.homestay.repository.AttachmentRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DamageReportManagerServiceTest {

    @Mock DamageReportRepository damageReportRepository;
    @Mock AttachmentRepository attachmentRepository;
    @Mock UserRepository userRepository;
    @Mock ReportPropertyScopeValidator scopeValidator;
    @Mock NotificationService notificationService;
    @Mock DamageFeeSettlementService damageFeeSettlementService;

    @InjectMocks DamageReportManagerService service;

    private User manager;
    private User customer;
    private Property property;
    private Room room;
    private Booking booking;
    private RoomInspection inspection;

    @BeforeEach
    void setUp() {
        manager = TestFixtures.user(User.Role.MANAGER);
        customer = TestFixtures.user(User.Role.CUSTOMER);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
        booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_INSPECTION);
        inspection = TestFixtures.inspection(booking, property, room);
    }

    @Test
    void approve_escalated_keepsPendingAndDoesNotSettle() {
        DamageReport dr = TestFixtures.damageReport(
                booking, inspection, new BigDecimal("6000000"), true);
        when(damageReportRepository.findDetailById(dr.getId())).thenReturn(Optional.of(dr));
        when(damageReportRepository.save(any(DamageReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByRole(User.Role.ADMIN)).thenReturn(List.of(TestFixtures.user(User.Role.ADMIN)));

        ApproveDamageReportRequest req = new ApproveDamageReportRequest();
        req.setApprovedAmount(new BigDecimal("6000000"));
        req.setNote("Escalate");

        DamageReportDetailResponse resp = service.approveForManager(manager, dr.getId(), req);

        assertEquals(DamageReport.Status.PENDING_APPROVAL.name(), resp.getStatus());
        assertEquals(manager, dr.getApprovedBy());
        verify(damageFeeSettlementService, never()).applyApprovedFee(any(), any());
        verify(notificationService).sendNotification(
                any(UUID.class), any(), eq("Báo cáo hư hại cần Admin duyệt"), any(), eq(dr.getId()), eq("DamageReport"));
    }

    @Test
    void approve_belowThreshold_setsApprovedAndSettles() {
        DamageReport dr = TestFixtures.damageReport(
                booking, inspection, new BigDecimal("2000000"), false);
        when(damageReportRepository.findDetailById(dr.getId())).thenReturn(Optional.of(dr));
        when(damageReportRepository.save(any(DamageReport.class))).thenAnswer(inv -> inv.getArgument(0));

        ApproveDamageReportRequest req = new ApproveDamageReportRequest();
        req.setApprovedAmount(new BigDecimal("1800000"));

        DamageReportDetailResponse resp = service.approveForManager(manager, dr.getId(), req);

        assertEquals(DamageReport.Status.APPROVED.name(), resp.getStatus());
        assertEquals(0, new BigDecimal("1800000").compareTo(dr.getApprovedAmount()));
        verify(damageFeeSettlementService).applyApprovedFee(eq(dr), eq(new BigDecimal("1800000")));
    }

    @Test
    void listAndDetailAndReject() {
        DamageReport dr = TestFixtures.damageReport(
                booking, inspection, new BigDecimal("500000"), false);
        when(damageReportRepository.findForManagerBoard(
                eq(property.getId()), any(), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(dr)));
        assertEquals(1, service.listForManager(manager, property.getId(), null, null, null, 0, 10)
                .getContent().size());

        when(damageReportRepository.findDetailById(dr.getId())).thenReturn(Optional.of(dr));
        service.getDetailForManager(manager, dr.getId());

        when(damageReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        com.homestay.dto.request.RejectDamageReportRequest reject =
                new com.homestay.dto.request.RejectDamageReportRequest();
        reject.setReason("Insufficient evidence for claim");
        inspection.setInspectedBy(TestFixtures.user(User.Role.EMPLOYEE));
        service.rejectForManager(manager, dr.getId(), reject);
        assertEquals(DamageReport.Status.DRAFT, dr.getStatus());
    }
}
