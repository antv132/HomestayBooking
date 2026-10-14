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
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.FloorRepository;
import com.homestay.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FloorService {

    private final FloorRepository floorRepository;
    private final PropertyRepository propertyRepository;
    private final ReportPropertyScopeValidator scopeValidator;

    // ── SCR-28 v1: Manager-scoped operations ─────────────────────────────────

    @Transactional(readOnly = true)
    public PropertyStructureResponse getStructureForManager(User manager, UUID propertyId) {
        scopeValidator.validateManagerAccess(manager, propertyId);
        return buildStructure(propertyId);
    }

    @Transactional(readOnly = true)
    public List<FloorResponse> getByPropertyForManager(User manager, UUID propertyId) {
        scopeValidator.validateManagerAccess(manager, propertyId);
        return getByProperty(propertyId);
    }

    @Transactional
    public FloorResponse createForManager(User manager, CreateFloorRequest request) {
        UUID propertyId = UUID.fromString(request.getPropertyId());
        scopeValidator.validateManagerAccess(manager, propertyId);
        return create(request);
    }

    @Transactional
    public FloorResponse updateForManager(User manager, UUID propertyId, UUID floorId, UpdateFloorRequest request) {
        Floor floor = findFloorById(floorId);
        assertFloorBelongsToProperty(floor, propertyId);
        scopeValidator.validateManagerAccess(manager, floor.getProperty().getId());
        return update(floorId, request);
    }

    @Transactional
    public void deleteForManager(User manager, UUID propertyId, UUID floorId) {
        Floor floor = findFloorById(floorId);
        assertFloorBelongsToProperty(floor, propertyId);
        scopeValidator.validateManagerAccess(manager, floor.getProperty().getId());
        delete(floorId);
    }

    // ── Legacy (SCR-37/38) — no assignment scope ─────────────────────────────

    public List<FloorResponse> getByProperty(UUID propertyId) {
        Property property = findPropertyById(propertyId);
        return floorRepository.findByPropertyOrderByFloorNumberAsc(property)
                .stream().map(FloorResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PropertyStructureResponse getStructure(UUID propertyId) {
        findPropertyById(propertyId);
        return buildStructure(propertyId);
    }

    @Transactional
    public FloorResponse create(CreateFloorRequest request) {
        UUID propertyId = UUID.fromString(request.getPropertyId());
        Property property = findPropertyById(propertyId);

        if (floorRepository.existsByPropertyAndFloorNumber(property, request.getFloorNumber())) {
            throw new ConflictException("Tầng " + request.getFloorNumber() + " đã tồn tại trong property này");
        }

        Floor floor = new Floor();
        floor.setProperty(property);
        floor.setFloorNumber(request.getFloorNumber());
        floor.setDescription(request.getDescription());
        floorRepository.save(floor);

        return FloorResponse.fromEntity(floor);
    }

    @Transactional
    public FloorResponse update(UUID floorId, UpdateFloorRequest request) {
        Floor floor = findFloorById(floorId);

        if (request.getFloorNumber() != null
                && !request.getFloorNumber().equals(floor.getFloorNumber())
                && floorRepository.existsByPropertyAndFloorNumber(floor.getProperty(), request.getFloorNumber())) {
            throw new ConflictException("Tầng " + request.getFloorNumber() + " đã tồn tại trong property này");
        }

        if (request.getFloorNumber() != null) {
            floor.setFloorNumber(request.getFloorNumber());
        }
        if (request.getDescription() != null) {
            floor.setDescription(request.getDescription());
        }

        floorRepository.save(floor);
        return FloorResponse.fromEntity(floor);
    }

    @Transactional
    public void delete(UUID floorId) {
        Floor floor = findFloorById(floorId);

        if (floor.getRooms() != null && floor.getRooms().stream().anyMatch(r -> r.getDeletedAt() == null)) {
            throw new ConflictException("Không thể xóa tầng đang có phòng. Vui lòng xóa phòng trước.");
        }

        floorRepository.delete(floor);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private PropertyStructureResponse buildStructure(UUID propertyId) {
        Property property = findPropertyById(propertyId);
        List<Floor> floors = floorRepository.findByPropertyIdWithRooms(propertyId);

        PropertyStructureResponse res = new PropertyStructureResponse();
        res.setPropertyId(property.getId());
        res.setPropertyName(property.getName());

        List<PropertyStructureResponse.FloorNode> floorNodes = floors.stream()
                .map(f -> {
                    PropertyStructureResponse.FloorNode fn = new PropertyStructureResponse.FloorNode();
                    fn.setId(f.getId());
                    fn.setFloorNumber(f.getFloorNumber());
                    fn.setDescription(f.getDescription());

                    List<PropertyStructureResponse.RoomNode> roomNodes = (f.getRooms() == null)
                            ? List.of()
                            : f.getRooms().stream()
                                    .filter(r -> r.getDeletedAt() == null)
                                    .sorted(Comparator.comparing(Room::getRoomNumber))
                                    .map(r -> {
                                        PropertyStructureResponse.RoomNode rn = new PropertyStructureResponse.RoomNode();
                                        rn.setId(r.getId());
                                        rn.setRoomNumber(r.getRoomNumber());
                                        rn.setRoomType(r.getRoomType());
                                        rn.setStatus(r.getStatus() != null ? r.getStatus().name() : null);
                                        rn.setPricePerNight(r.getPricePerNight() != null
                                                ? r.getPricePerNight().doubleValue() : null);
                                        rn.setCapacity(r.getCapacity());
                                        return rn;
                                    }).collect(Collectors.toList());

                    fn.setRoomCount(roomNodes.size());
                    fn.setRooms(roomNodes);
                    return fn;
                }).collect(Collectors.toList());

        res.setFloors(floorNodes);
        return res;
    }

    private void assertFloorBelongsToProperty(Floor floor, UUID propertyId) {
        if (propertyId != null && !floor.getProperty().getId().equals(propertyId)) {
            throw new ResourceNotFoundException("Không tìm thấy tầng trong property này");
        }
    }

    private Floor findFloorById(UUID floorId) {
        return floorRepository.findById(floorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tầng"));
    }

    private Property findPropertyById(UUID id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy property"));
    }
}
