package com.homestay.service;

import com.homestay.dto.request.AssignInspectorRequest;
import com.homestay.dto.response.InspectionSummaryResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.EmployeePropertyAssignment;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.RoomInspection;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ConflictException;
import com.homestay.repository.EmployeePropertyAssignmentRepository;
import com.homestay.repository.RoomInspectionRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomInspectionManagerServiceTest {

    @Mock RoomInspectionRepository inspectionRepository;
    @Mock EmployeePropertyAssignmentRepository employeeAssignmentRepository;
    @Mock ReportPropertyScopeValidator scopeValidator;

    private RoomInspectionManagerService service;
    private User manager;
    private User employee;
    private Property property;
    private Room room;
    private Booking booking;
    private RoomInspection inspection;

    @BeforeEach
    void setUp() {
        service = new RoomInspectionManagerService(
                inspectionRepository, employeeAssignmentRepository, scopeValidator);
        manager = TestFixtures.user(User.Role.MANAGER);
        employee = TestFixtures.user(User.Role.EMPLOYEE);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
        booking = TestFixtures.booking(TestFixtures.user(User.Role.CUSTOMER), room, Booking.Status.CHECKED_OUT);
        inspection = TestFixtures.inspection(booking, property, room);
        inspection.setStatus(RoomInspection.Status.PENDING);
    }

    @Test
    void listForManager() {
        when(inspectionRepository.findForManagerBoard(
                eq(property.getId()), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inspection)));

        PageResponse<InspectionSummaryResponse> page =
                service.listForManager(manager, property.getId(), null, null, 0, 10);
        assertEquals(1, page.getContent().size());
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
    }

    @Test
    void assignInspector_pendingToInProgress() {
        when(inspectionRepository.findById(inspection.getId())).thenReturn(Optional.of(inspection));
        when(employeeAssignmentRepository.existsByEmployeeIdAndPropertyIdAndStatus(
                employee.getId(), property.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(true);
        when(employeeAssignmentRepository.findActiveEmployeesByPropertyId(
                property.getId(), EmployeePropertyAssignment.Status.ACTIVE, User.Role.EMPLOYEE))
                .thenReturn(List.of(employee));
        when(inspectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AssignInspectorRequest req = new AssignInspectorRequest();
        req.setEmployeeId(employee.getId());
        InspectionSummaryResponse res = service.assignInspector(manager, inspection.getId(), req);
        assertEquals(RoomInspection.Status.IN_PROGRESS.name(), res.getStatus());
        assertEquals(employee.getId(), res.getInspectorId());
    }

    @Test
    void assignInspector_rejectsCompleted() {
        inspection.setStatus(RoomInspection.Status.PASSED);
        when(inspectionRepository.findById(inspection.getId())).thenReturn(Optional.of(inspection));
        AssignInspectorRequest req = new AssignInspectorRequest();
        req.setEmployeeId(employee.getId());
        assertThrows(ConflictException.class, () ->
                service.assignInspector(manager, inspection.getId(), req));
    }

    @Test
    void assignInspector_employeeNotInProperty() {
        when(inspectionRepository.findById(inspection.getId())).thenReturn(Optional.of(inspection));
        when(employeeAssignmentRepository.existsByEmployeeIdAndPropertyIdAndStatus(
                employee.getId(), property.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(false);
        AssignInspectorRequest req = new AssignInspectorRequest();
        req.setEmployeeId(employee.getId());
        assertThrows(BusinessException.class, () ->
                service.assignInspector(manager, inspection.getId(), req));
    }
}
