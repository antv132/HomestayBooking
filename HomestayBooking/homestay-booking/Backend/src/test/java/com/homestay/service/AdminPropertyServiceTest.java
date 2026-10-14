package com.homestay.service;

import com.homestay.dto.request.AdminCreatePropertyRequest;
import com.homestay.dto.request.AdminUpdatePropertyRequest;
import com.homestay.entity.ManagerPropertyAssignment;
import com.homestay.entity.Property;
import com.homestay.entity.User;
import com.homestay.exception.ConflictException;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.PropertyRepository;
import com.homestay.repository.UserRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPropertyServiceTest {

    @Mock PropertyRepository propertyRepository;
    @Mock ManagerPropertyAssignmentRepository assignmentRepository;
    @Mock UserRepository userRepository;
    @Mock ActivityLogService activityLogService;

    private AdminPropertyService service;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new AdminPropertyService(
                propertyRepository, assignmentRepository, userRepository, activityLogService);
        admin = TestFixtures.user(User.Role.ADMIN);
    }

    @Test
    void create_defaultsInactive() {
        AdminCreatePropertyRequest req = new AdminCreatePropertyRequest();
        req.setName("Sunset");
        req.setAddress("Da Nang");
        when(propertyRepository.save(any(Property.class))).thenAnswer(inv -> {
            Property p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        var res = service.createProperty(req, admin);
        assertEquals("INACTIVE", res.getStatus());

        ArgumentCaptor<Property> cap = ArgumentCaptor.forClass(Property.class);
        verify(propertyRepository).save(cap.capture());
        assertEquals(Property.Status.INACTIVE, cap.getValue().getStatus());
    }

    @Test
    void update_activateWithoutManager_conflicts() {
        UUID id = UUID.randomUUID();
        Property property = new Property();
        property.setId(id);
        property.setName("P");
        property.setAddress("A");
        property.setStatus(Property.Status.INACTIVE);

        when(propertyRepository.findById(id)).thenReturn(Optional.of(property));
        when(assignmentRepository.existsByPropertyIdAndStatus(id, ManagerPropertyAssignment.Status.ACTIVE))
                .thenReturn(false);

        AdminUpdatePropertyRequest req = new AdminUpdatePropertyRequest();
        req.setStatus("ACTIVE");

        assertThrows(ConflictException.class, () -> service.updateProperty(id, req, admin));
        verify(propertyRepository, never()).save(any());
    }

    @Test
    void assignManager_rejectsNonActiveUser() {
        UUID propertyId = UUID.randomUUID();
        Property property = new Property();
        property.setId(propertyId);
        property.setName("P");
        property.setAddress("A");
        property.setStatus(Property.Status.INACTIVE);

        User manager = TestFixtures.user(User.Role.MANAGER);
        manager.setStatus(User.Status.SUSPENDED);

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(userRepository.findById(manager.getId())).thenReturn(Optional.of(manager));

        assertThrows(ConflictException.class,
                () -> service.assignManager(propertyId, manager.getId(), admin));
    }

    @Test
    void assignManager_swapsActiveAssignment() {
        UUID propertyId = UUID.randomUUID();
        Property property = new Property();
        property.setId(propertyId);
        property.setName("P");
        property.setAddress("A");
        property.setStatus(Property.Status.INACTIVE);

        User oldManager = TestFixtures.user(User.Role.MANAGER);
        User newManager = TestFixtures.user(User.Role.MANAGER);
        newManager.setStatus(User.Status.ACTIVE);

        ManagerPropertyAssignment old = new ManagerPropertyAssignment();
        old.setId(UUID.randomUUID());
        old.setManager(oldManager);
        old.setProperty(property);
        old.setStatus(ManagerPropertyAssignment.Status.ACTIVE);

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(userRepository.findById(newManager.getId())).thenReturn(Optional.of(newManager));
        when(assignmentRepository.findActiveByPropertyIds(List.of(propertyId), ManagerPropertyAssignment.Status.ACTIVE))
                .thenReturn(List.of(old));
        when(assignmentRepository.save(any(ManagerPropertyAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.assignManager(propertyId, newManager.getId(), admin);

        assertEquals(ManagerPropertyAssignment.Status.INACTIVE, old.getStatus());
        verify(assignmentRepository).saveAll(List.of(old));
        verify(assignmentRepository).save(any(ManagerPropertyAssignment.class));
    }
}
