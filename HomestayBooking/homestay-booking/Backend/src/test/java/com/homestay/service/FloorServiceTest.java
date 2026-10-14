package com.homestay.service;

import com.homestay.dto.request.CreateFloorRequest;
import com.homestay.dto.request.UpdateFloorRequest;
import com.homestay.dto.response.FloorResponse;
import com.homestay.dto.response.PropertyStructureResponse;
import com.homestay.entity.Floor;
import com.homestay.entity.Property;
import com.homestay.entity.Room;
import com.homestay.entity.User;
import com.homestay.exception.ConflictException;
import com.homestay.exception.ForbiddenException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.FloorRepository;
import com.homestay.repository.PropertyRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FloorServiceTest {

    @Mock FloorRepository floorRepository;
    @Mock PropertyRepository propertyRepository;
    @Mock ReportPropertyScopeValidator scopeValidator;

    private FloorService service;
    private User manager;
    private Property property;
    private Floor floor;

    @BeforeEach
    void setUp() {
        service = new FloorService(floorRepository, propertyRepository, scopeValidator);
        manager = TestFixtures.user(User.Role.MANAGER);
        property = TestFixtures.property();
        property.setStatus(Property.Status.ACTIVE);
        property.setAddress("12 Trần Phú, Đà Nẵng");
        floor = new Floor();
        floor.setId(UUID.randomUUID());
        floor.setProperty(property);
        floor.setFloorNumber(1);
        floor.setDescription("Tầng 1");
        floor.setRooms(new ArrayList<>());
    }

    @Test
    void createForManager_duplicateFloorNumber_throwsConflict() {
        CreateFloorRequest req = new CreateFloorRequest();
        req.setPropertyId(property.getId().toString());
        req.setFloorNumber(1);
        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(floorRepository.existsByPropertyAndFloorNumber(property, 1)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.createForManager(manager, req));
        verify(scopeValidator).validateManagerAccess(manager, property.getId());
        verify(floorRepository, never()).save(any());
    }

    @Test
    void createForManager_valid_savesFloor() {
        CreateFloorRequest req = new CreateFloorRequest();
        req.setPropertyId(property.getId().toString());
        req.setFloorNumber(2);
        req.setDescription("Tầng 2");
        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(floorRepository.existsByPropertyAndFloorNumber(property, 2)).thenReturn(false);
        when(floorRepository.save(any(Floor.class))).thenAnswer(inv -> {
            Floor f = inv.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });

        FloorResponse res = service.createForManager(manager, req);
        assertEquals(2, res.getFloorNumber());
        verify(floorRepository).save(any(Floor.class));
    }

    @Test
    void deleteForManager_floorWithRooms_throwsConflict() {
        Room room = TestFixtures.room(property);
        floor.getRooms().add(room);
        when(floorRepository.findById(floor.getId())).thenReturn(Optional.of(floor));

        assertThrows(ConflictException.class,
                () -> service.deleteForManager(manager, property.getId(), floor.getId()));
        verify(floorRepository, never()).delete(any());
    }

    @Test
    void deleteForManager_emptyFloor_deletes() {
        when(floorRepository.findById(floor.getId())).thenReturn(Optional.of(floor));

        service.deleteForManager(manager, property.getId(), floor.getId());
        verify(floorRepository).delete(floor);
    }

    @Test
    void updateForManager_wrongPropertyPath_throwsNotFound() {
        when(floorRepository.findById(floor.getId())).thenReturn(Optional.of(floor));
        UUID otherProperty = UUID.randomUUID();
        UpdateFloorRequest req = new UpdateFloorRequest();
        req.setDescription("x");

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateForManager(manager, otherProperty, floor.getId(), req));
    }

    @Test
    void getStructureForManager_unassigned_propagatesForbidden() {
        doThrow(new ForbiddenException("Bạn không có quyền truy cập property này"))
                .when(scopeValidator).validateManagerAccess(eq(manager), eq(property.getId()));

        assertThrows(ForbiddenException.class,
                () -> service.getStructureForManager(manager, property.getId()));
    }

    @Test
    void getStructure_returnsFloorsSorted() {
        Floor f2 = new Floor();
        f2.setId(UUID.randomUUID());
        f2.setProperty(property);
        f2.setFloorNumber(2);
        f2.setRooms(List.of());
        when(propertyRepository.findById(property.getId())).thenReturn(Optional.of(property));
        when(floorRepository.findByPropertyIdWithRooms(property.getId())).thenReturn(List.of(floor, f2));

        PropertyStructureResponse tree = service.getStructure(property.getId());
        assertEquals(property.getName(), tree.getPropertyName());
        assertEquals(2, tree.getFloors().size());
        assertEquals(1, tree.getFloors().get(0).getFloorNumber());
    }

    @Test
    void updateForManager_duplicateNumber_throwsConflict() {
        when(floorRepository.findById(floor.getId())).thenReturn(Optional.of(floor));
        when(floorRepository.existsByPropertyAndFloorNumber(property, 3)).thenReturn(true);
        UpdateFloorRequest req = new UpdateFloorRequest();
        req.setFloorNumber(3);

        assertThrows(ConflictException.class,
                () -> service.updateForManager(manager, property.getId(), floor.getId(), req));
    }
}
