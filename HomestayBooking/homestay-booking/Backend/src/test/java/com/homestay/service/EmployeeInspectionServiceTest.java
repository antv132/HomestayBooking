package com.homestay.service;

import com.homestay.dto.request.EmployeeInspectionResultRequest;
import com.homestay.dto.response.EmployeeInspectionResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.EmployeePropertyAssignment;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.RoomInspection;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.repository.EmployeePropertyAssignmentRepository;
import com.homestay.repository.RoomInspectionRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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
class EmployeeInspectionServiceTest {

    @Mock RoomInspectionRepository roomInspectionRepository;
    @Mock EmployeePropertyAssignmentRepository employeePropertyAssignmentRepository;

    @InjectMocks EmployeeInspectionService service;

    private User employee;
    private User otherEmployee;
    private Property property;
    private Room room;
    private Booking booking;
    private RoomInspection pending;

    @BeforeEach
    void setUp() {
        employee = TestFixtures.user(User.Role.EMPLOYEE);
        otherEmployee = TestFixtures.user(User.Role.EMPLOYEE);
        property = TestFixtures.property();
        room = TestFixtures.room(property);
        booking = TestFixtures.booking(
                TestFixtures.user(User.Role.CUSTOMER), room, Booking.Status.PENDING_INSPECTION);
        pending = TestFixtures.inspection(booking, property, room);
        pending.setStatus(RoomInspection.Status.PENDING);
    }

    @Test
    void list_filtersByStatusWhenProvided() {
        when(employeePropertyAssignmentRepository.findPropertyIdsByEmployeeIdAndStatus(
                employee.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of(property.getId()));
        when(roomInspectionRepository.findForEmployee(
                eq(List.of(property.getId())),
                eq(List.of(RoomInspection.Status.PENDING)),
                eq(employee.getId()),
                any()))
                .thenReturn(new PageImpl<>(List.of(pending), PageRequest.of(0, 10), 1));

        PageResponse<EmployeeInspectionResponse> page =
                service.list(employee, "PENDING", PageRequest.of(0, 10));

        assertEquals(1, page.getContent().size());
        assertEquals(pending.getId().toString(), page.getContent().get(0).getId());
        assertEquals("101", page.getContent().get(0).getRoom().getRoomNumber());
    }

    @Test
    void pass_setsPassedAndInspectedBy() {
        when(employeePropertyAssignmentRepository.findPropertyIdsByEmployeeIdAndStatus(
                employee.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of(property.getId()));
        when(roomInspectionRepository.findByIdAndPropertyIdIn(pending.getId(), List.of(property.getId())))
                .thenReturn(Optional.of(pending));
        when(roomInspectionRepository.save(any(RoomInspection.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeInspectionResultRequest req = new EmployeeInspectionResultRequest();
        req.setNote("All good");

        service.pass(employee, pending.getId(), req);

        assertEquals(RoomInspection.Status.PASSED, pending.getStatus());
        assertEquals(employee, pending.getInspectedBy());
        verify(roomInspectionRepository).save(pending);
    }

    @Test
    void fail_requiresNote() {
        EmployeeInspectionResultRequest req = new EmployeeInspectionResultRequest();
        assertThrows(BusinessException.class, () -> service.fail(employee, pending.getId(), req));
    }

    @Test
    void fail_blocksWhenClaimedByAnotherEmployee() {
        pending.setStatus(RoomInspection.Status.IN_PROGRESS);
        pending.setInspectedBy(otherEmployee);

        when(employeePropertyAssignmentRepository.findPropertyIdsByEmployeeIdAndStatus(
                employee.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of(property.getId()));
        when(roomInspectionRepository.findByIdAndPropertyIdIn(pending.getId(), List.of(property.getId())))
                .thenReturn(Optional.of(pending));

        EmployeeInspectionResultRequest req = new EmployeeInspectionResultRequest();
        req.setNote("Broken TV");

        BusinessException ex = assertThrows(
                BusinessException.class, () -> service.fail(employee, pending.getId(), req));
        assertEquals("Pass/Fail only when the inspection is assigned to you", ex.getMessage());
    }

    @Test
    void pass_blocksWhenPendingButAssignedToAnotherEmployee() {
        pending.setStatus(RoomInspection.Status.PENDING);
        pending.setInspectedBy(otherEmployee);

        when(employeePropertyAssignmentRepository.findPropertyIdsByEmployeeIdAndStatus(
                employee.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of(property.getId()));
        when(roomInspectionRepository.findByIdAndPropertyIdIn(pending.getId(), List.of(property.getId())))
                .thenReturn(Optional.of(pending));

        BusinessException ex = assertThrows(
                BusinessException.class, () -> service.pass(employee, pending.getId(), null));
        assertEquals("Pass/Fail only when the inspection is assigned to you", ex.getMessage());
    }

    @Test
    void fail_setsFailedWithDamage() {
        when(employeePropertyAssignmentRepository.findPropertyIdsByEmployeeIdAndStatus(
                employee.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of(property.getId()));
        when(roomInspectionRepository.findByIdAndPropertyIdIn(pending.getId(), List.of(property.getId())))
                .thenReturn(Optional.of(pending));
        when(roomInspectionRepository.save(any(RoomInspection.class))).thenAnswer(inv -> inv.getArgument(0));

        EmployeeInspectionResultRequest req = new EmployeeInspectionResultRequest();
        req.setNote("Cracked TV");

        service.fail(employee, pending.getId(), req);

        assertEquals(RoomInspection.Status.FAILED_WITH_DAMAGE, pending.getStatus());
        assertEquals(employee, pending.getInspectedBy());
    }
}
