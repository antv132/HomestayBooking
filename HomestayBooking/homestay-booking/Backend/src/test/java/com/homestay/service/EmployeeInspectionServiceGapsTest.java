package com.homestay.service;

import com.homestay.dto.request.EmployeeInspectionResultRequest;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.Booking;
import com.homestay.entity.ChecklistItemDefinition;
import com.homestay.entity.EmployeePropertyAssignment;
import com.homestay.entity.InspectionChecklistAnswer;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.RoomInspection;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.repository.ChecklistItemDefinitionRepository;
import com.homestay.repository.EmployeePropertyAssignmentRepository;
import com.homestay.repository.InspectionChecklistAnswerRepository;
import com.homestay.repository.RoomInspectionRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeInspectionServiceGapsTest {

    @Mock RoomInspectionRepository roomInspectionRepository;
    @Mock EmployeePropertyAssignmentRepository employeePropertyAssignmentRepository;
    @Mock ChecklistItemDefinitionRepository checklistItemDefinitionRepository;
    @Mock InspectionChecklistAnswerRepository checklistAnswerRepository;

    private EmployeeInspectionService service;
    private User employee;
    private Property property;
    private RoomInspection pending;
    private ChecklistItemDefinition def;

    @BeforeEach
    void setUp() {
        service = new EmployeeInspectionService(
                roomInspectionRepository,
                employeePropertyAssignmentRepository,
                checklistItemDefinitionRepository,
                checklistAnswerRepository);
        employee = TestFixtures.user(User.Role.EMPLOYEE);
        property = TestFixtures.property();
        Room room = TestFixtures.room(property);
        Booking booking = TestFixtures.booking(
                TestFixtures.user(User.Role.CUSTOMER), room, Booking.Status.PENDING_INSPECTION);
        pending = TestFixtures.inspection(booking, property, room);
        pending.setStatus(RoomInspection.Status.PENDING);
        pending.setInspectedBy(null);

        def = new ChecklistItemDefinition();
        def.setId(UUID.randomUUID());
        def.setCode("tv");
        def.setLabel("TV");
        def.setIcon("tv");
        def.setIsActive(true);
        def.setSortOrder(1);
    }

    @Test
    void list_emptyAssignments() {
        when(employeePropertyAssignmentRepository.findPropertyIdsByEmployeeIdAndStatus(
                employee.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of());
        PageResponse<?> page = service.list(employee, null, PageRequest.of(0, 10));
        assertEquals(0, page.getTotalElements());
    }

    @Test
    void list_rejectsInvalidStatus() {
        when(employeePropertyAssignmentRepository.findPropertyIdsByEmployeeIdAndStatus(
                employee.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of(property.getId()));
        assertThrows(BusinessException.class, () ->
                service.list(employee, "PASSED", PageRequest.of(0, 10)));
    }

    @Test
    void listActiveChecklistItems() {
        when(checklistItemDefinitionRepository.findByIsActiveTrueAndPropertyIsNullOrderBySortOrderAsc())
                .thenReturn(List.of(def));
        assertEquals(1, service.listActiveChecklistItems().size());
    }

    @Test
    void listAnswers() {
        InspectionChecklistAnswer answer = new InspectionChecklistAnswer();
        answer.setId(UUID.randomUUID());
        answer.setChecklistItem(def);
        answer.setPassed(true);
        when(checklistAnswerRepository.findByInspectionIdOrderByChecklistItem_SortOrderAsc(pending.getId()))
                .thenReturn(List.of(answer));
        assertEquals(1, service.listAnswers(pending.getId()).size());
    }

    @Test
    void pass_withChecklistAnswers() {
        when(employeePropertyAssignmentRepository.findPropertyIdsByEmployeeIdAndStatus(
                employee.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of(property.getId()));
        when(roomInspectionRepository.findByIdAndPropertyIdIn(pending.getId(), List.of(property.getId())))
                .thenReturn(Optional.of(pending));
        when(checklistItemDefinitionRepository.findAllById(any())).thenReturn(List.of(def));
        when(checklistAnswerRepository.save(any())).thenAnswer(inv -> {
            InspectionChecklistAnswer a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        when(roomInspectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EmployeeInspectionResultRequest req = new EmployeeInspectionResultRequest();
        req.setNote("All items checked");
        EmployeeInspectionResultRequest.Answer ans = new EmployeeInspectionResultRequest.Answer();
        ans.setItemId(def.getId());
        ans.setPassed(true);
        req.setAnswers(List.of(ans));

        service.pass(employee, pending.getId(), req);
        assertEquals(RoomInspection.Status.PASSED, pending.getStatus());
        assertTrue(pending.getNote().contains("Checklist"));
        verify(checklistAnswerRepository).deleteByInspectionId(pending.getId());
    }

    @Test
    void pass_withLegacyChecklist() {
        when(employeePropertyAssignmentRepository.findPropertyIdsByEmployeeIdAndStatus(
                employee.getId(), EmployeePropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of(property.getId()));
        when(roomInspectionRepository.findByIdAndPropertyIdIn(pending.getId(), List.of(property.getId())))
                .thenReturn(Optional.of(pending));
        when(checklistItemDefinitionRepository.findByCodeAndPropertyIsNull("tv")).thenReturn(Optional.of(def));
        when(checklistItemDefinitionRepository.findAllById(any())).thenReturn(List.of(def));
        when(checklistAnswerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roomInspectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EmployeeInspectionResultRequest req = new EmployeeInspectionResultRequest();
        req.setNote("Legacy path");
        EmployeeInspectionResultRequest.LegacyChecklist legacy =
                new EmployeeInspectionResultRequest.LegacyChecklist();
        legacy.setTv(true);
        req.setChecklist(legacy);

        service.pass(employee, pending.getId(), req);
        assertEquals(RoomInspection.Status.PASSED, pending.getStatus());
    }
}
