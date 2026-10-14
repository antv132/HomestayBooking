package com.homestay.service;

import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.PropertyDetailResponse;
import com.homestay.dto.response.PropertyResponse;
import com.homestay.entity.Floor;
import com.homestay.entity.Property;
import com.homestay.entity.User;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.FloorRepository;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.PropertyRepository;
import com.homestay.security.PropertyAccessValidator;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerPropertyServiceTest {

    @Mock ManagerPropertyAssignmentRepository assignmentRepository;
    @Mock PropertyRepository propertyRepository;
    @Mock FloorRepository floorRepository;
    @Mock PropertyAccessValidator propertyAccessValidator;

    private ManagerPropertyService service;
    private User manager;
    private Property property;

    @BeforeEach
    void setUp() {
        service = new ManagerPropertyService(
                assignmentRepository, propertyRepository, floorRepository, propertyAccessValidator);
        manager = TestFixtures.user(User.Role.MANAGER);
        property = TestFixtures.property();
        property.setStatus(Property.Status.ACTIVE);
        property.setAddress("Da Nang Beach");
    }

    @Test
    void listAssigned_filtersBySearchAndStatus() {
        Property other = TestFixtures.property();
        other.setName("Other Place");
        other.setAddress("Hanoi");
        other.setStatus(Property.Status.INACTIVE);
        when(assignmentRepository.findAssignedPropertiesByManagerId(manager.getId()))
                .thenReturn(List.of(property, other));

        PageResponse<PropertyResponse> page = service.listAssigned(
                manager, "beach", "ACTIVE", PageRequest.of(0, 10));
        assertEquals(1, page.getContent().size());
        assertEquals(property.getId(), page.getContent().get(0).getId());
    }

    @Test
    void listAssigned_invalidStatusIgnored() {
        when(assignmentRepository.findAssignedPropertiesByManagerId(manager.getId()))
                .thenReturn(List.of(property));
        assertEquals(1, service.listAssigned(manager, null, "NOPE", PageRequest.of(0, 10))
                .getTotalElements());
    }

    @Test
    void getDetail_loadsFloors() {
        Floor floor = TestFixtures.floor(property);
        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(floorRepository.findByPropertyIdWithRooms(property.getId())).thenReturn(List.of(floor));

        PropertyDetailResponse detail = service.getDetail(manager, property.getId());
        assertEquals(property.getId(), detail.getId());
        verify(propertyAccessValidator).assertManagerAssigned(manager, property.getId());
    }

    @Test
    void getById_notFound() {
        when(propertyRepository.findById(property.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getById(manager, property.getId()));
    }

    @Test
    void getById_ok() {
        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        assertEquals(property.getId(), service.getById(manager, property.getId()).getId());
    }
}
