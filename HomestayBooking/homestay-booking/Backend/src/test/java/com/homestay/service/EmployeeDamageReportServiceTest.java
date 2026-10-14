package com.homestay.service;

import com.homestay.dto.request.CreateEmployeeDamageReportRequest;
import com.homestay.entity.Booking;
import com.homestay.entity.DamageItem;
import com.homestay.entity.DamageReport;
import com.homestay.entity.ManagerPropertyAssignment;
import com.homestay.entity.Notification;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.RoomInspection;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.repository.AttachmentRepository;
import com.homestay.repository.DamageReportRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.RoomInspectionRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeDamageReportServiceTest {

    @Mock DamageReportRepository damageReportRepository;
    @Mock RoomInspectionRepository roomInspectionRepository;
    @Mock AttachmentRepository attachmentRepository;
    @Mock NotificationService notificationService;
    @Mock ManagerPropertyAssignmentRepository managerPropertyAssignmentRepository;

    private EmployeeDamageReportService service;
    private User employee;
    private User customer;
    private User manager;
    private Property property;
    private Room room;
    private Booking booking;
    private RoomInspection inspection;

    @BeforeEach
    void setUp() {
        service = new EmployeeDamageReportService(
                damageReportRepository,
                roomInspectionRepository,
                attachmentRepository,
                notificationService,
                managerPropertyAssignmentRepository);
        employee = TestFixtures.user(User.Role.EMPLOYEE);
        customer = TestFixtures.user(User.Role.CUSTOMER);
        manager = TestFixtures.user(User.Role.MANAGER);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
        booking = TestFixtures.booking(customer, room, Booking.Status.PENDING_INSPECTION);
        inspection = TestFixtures.inspection(booking, property, room);
        inspection.setInspectedBy(employee);
    }

    @Test
    void create_notifiesCustomerEmployeeAndManagers() {
        when(roomInspectionRepository.findFailedByIdForEmployee(
                eq(inspection.getId()), eq(employee.getId()), eq(RoomInspection.Status.FAILED_WITH_DAMAGE)))
                .thenReturn(Optional.of(inspection));
        when(damageReportRepository.existsByInspection_Id(inspection.getId())).thenReturn(false);
        when(damageReportRepository.save(any(DamageReport.class))).thenAnswer(inv -> {
            DamageReport dr = inv.getArgument(0);
            dr.setId(UUID.randomUUID());
            for (DamageItem item : dr.getItems()) {
                item.setId(UUID.randomUUID());
            }
            return dr;
        });
        when(attachmentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        ManagerPropertyAssignment mpa = new ManagerPropertyAssignment();
        mpa.setManager(manager);
        mpa.setProperty(property);
        mpa.setStatus(ManagerPropertyAssignment.Status.ACTIVE);
        when(managerPropertyAssignmentRepository.findActiveByPropertyIds(
                eq(List.of(property.getId())), eq(ManagerPropertyAssignment.Status.ACTIVE)))
                .thenReturn(List.of(mpa));

        CreateEmployeeDamageReportRequest req = new CreateEmployeeDamageReportRequest();
        req.setRoomId(room.getId());
        req.setInspectionId(inspection.getId());
        CreateEmployeeDamageReportRequest.Item item = new CreateEmployeeDamageReportRequest.Item();
        item.setName("Broken lamp");
        item.setEstimatedCost(new BigDecimal("250000"));
        req.setItems(List.of(item));
        CreateEmployeeDamageReportRequest.AttachmentRef att = new CreateEmployeeDamageReportRequest.AttachmentRef();
        att.setUrl("/uploads/damage/photo1.jpg");
        req.setAttachments(List.of(att));

        service.create(employee, req);

        ArgumentCaptor<UUID> userIds = ArgumentCaptor.forClass(UUID.class);
        verify(notificationService, times(3)).sendNotification(
                userIds.capture(),
                eq(Notification.Type.SYSTEM),
                any(),
                any(),
                any(UUID.class),
                eq("DamageReport"));

        List<UUID> notified = new ArrayList<>(userIds.getAllValues());
        assertEquals(3, notified.size());
        assertEquals(true, notified.contains(customer.getId()));
        assertEquals(true, notified.contains(employee.getId()));
        assertEquals(true, notified.contains(manager.getId()));
    }

    @Test
    void list_mapsPage() {
        DamageReport report = TestFixtures.damageReport(
                booking, inspection, new BigDecimal("250000"), false);
        report.setItems(new ArrayList<>());
        when(damageReportRepository.findForEmployee(
                eq(employee.getId()), eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(report)));

        assertEquals(1, service.list(employee, null, "  ", PageRequest.of(0, 10)).getContent().size());
    }

    @Test
    void listEligibleRooms() {
        when(roomInspectionRepository.findEligibleForDamageReport(
                employee.getId(), RoomInspection.Status.FAILED_WITH_DAMAGE))
                .thenReturn(List.of(inspection));
        assertEquals(1, service.listEligibleRooms(employee).size());
    }

    @Test
    void uploadPhotos_rejectsEmpty() {
        assertThrows(BusinessException.class, () -> service.uploadPhotos(employee, List.of()));
    }

    @Test
    void uploadPhotos_rejectsTooMany() {
        MultipartFile f = org.mockito.Mockito.mock(MultipartFile.class);
        assertThrows(BusinessException.class, () ->
                service.uploadPhotos(employee, List.of(f, f, f, f, f, f)));
    }
}
