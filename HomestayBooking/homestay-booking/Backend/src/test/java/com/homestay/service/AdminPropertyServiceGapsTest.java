package com.homestay.service;

import com.homestay.dto.request.AdminUpdatePropertyRequest;
import com.homestay.dto.response.AdminPropertyResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.ManagerPropertyAssignment;
import com.homestay.entity.Property;
import com.homestay.entity.User;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.PropertyRepository;
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
class AdminPropertyServiceGapsTest {

    @Mock PropertyRepository propertyRepository;
    @Mock ManagerPropertyAssignmentRepository assignmentRepository;
    @Mock UserRepository userRepository;
    @Mock ActivityLogService activityLogService;

    private AdminPropertyService service;
    private User admin;
    private Property property;
    private User manager;

    @BeforeEach
    void setUp() {
        service = new AdminPropertyService(
                propertyRepository, assignmentRepository, userRepository, activityLogService);
        admin = TestFixtures.user(User.Role.ADMIN);
        property = TestFixtures.property();
        property.setStatus(Property.Status.INACTIVE);
        property.setAddress("12 Beach Rd");
        manager = TestFixtures.user(User.Role.MANAGER);
    }

    @Test
    void listProperties_joinsManagers() {
        when(propertyRepository.searchForAdmin(eq("beach"), eq(Property.Status.INACTIVE), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(property)));
        ManagerPropertyAssignment mpa = new ManagerPropertyAssignment();
        mpa.setProperty(property);
        mpa.setManager(manager);
        mpa.setStatus(ManagerPropertyAssignment.Status.ACTIVE);
        when(assignmentRepository.findActiveByPropertyIds(
                List.of(property.getId()), ManagerPropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of(mpa));

        PageResponse<AdminPropertyResponse> page =
                service.listProperties(" beach ", "INACTIVE", null, PageRequest.of(0, 10));
        assertEquals(1, page.getContent().size());
        assertEquals(manager.getId(), page.getContent().get(0).getManagerId());
    }

    @Test
    void getProperty_ok() {
        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(assignmentRepository.findActiveByPropertyIds(
                List.of(property.getId()), ManagerPropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of());
        assertEquals(property.getId(), service.getProperty(property.getId()).getId());
    }

    @Test
    void getProperty_missing() {
        when(propertyRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getProperty(UUID.randomUUID()));
    }

    @Test
    void updateProperty_logsUpdated() {
        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assignmentRepository.findActiveByPropertyIds(
                List.of(property.getId()), ManagerPropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of());

        AdminUpdatePropertyRequest req = new AdminUpdatePropertyRequest();
        req.setName("Renamed Homestay");
        req.setAddress("New Address");
        req.setDescription("  ");
        AdminPropertyResponse res = service.updateProperty(property.getId(), req, admin);
        assertEquals("Renamed Homestay", res.getName());
        verify(activityLogService).log(eq(admin), eq(AdminPropertyService.PROPERTY_UPDATED),
                eq("Property"), eq(property.getId()), any());
    }

    @Test
    void updateProperty_activateWithManager() {
        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(assignmentRepository.existsByPropertyIdAndStatus(
                property.getId(), ManagerPropertyAssignment.Status.ACTIVE)).thenReturn(true);
        when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assignmentRepository.findActiveByPropertyIds(
                List.of(property.getId()), ManagerPropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of());

        AdminUpdatePropertyRequest req = new AdminUpdatePropertyRequest();
        req.setStatus("ACTIVE");
        AdminPropertyResponse res = service.updateProperty(property.getId(), req, admin);
        assertEquals("ACTIVE", res.getStatus());
        verify(activityLogService).log(eq(admin), eq(AdminPropertyService.PROPERTY_STATUS_CHANGED),
                eq("Property"), eq(property.getId()), any());
    }

    @Test
    void listAssignmentHistory() {
        when(propertyRepository.existsById(property.getId())).thenReturn(true);
        ManagerPropertyAssignment hist = new ManagerPropertyAssignment();
        hist.setId(UUID.randomUUID());
        hist.setProperty(property);
        hist.setManager(manager);
        hist.setStatus(ManagerPropertyAssignment.Status.INACTIVE);
        when(assignmentRepository.findHistoryByPropertyId(property.getId())).thenReturn(List.of(hist));

        assertEquals(1, service.listAssignmentHistory(property.getId(), PageRequest.of(0, 10))
                .getContent().size());
    }

    @Test
    void listAssignmentHistory_missingProperty() {
        when(propertyRepository.existsById(any())).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () ->
                service.listAssignmentHistory(UUID.randomUUID(), PageRequest.of(0, 5)));
    }
}
