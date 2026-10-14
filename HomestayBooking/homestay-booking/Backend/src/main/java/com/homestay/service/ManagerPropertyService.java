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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/** FR-06 US3 — Manager read-only properties scoped by ACTIVE assignment. */
@Service
@RequiredArgsConstructor
public class ManagerPropertyService {

    private final ManagerPropertyAssignmentRepository assignmentRepository;
    private final PropertyRepository propertyRepository;
    private final FloorRepository floorRepository;
    private final PropertyAccessValidator propertyAccessValidator;

    @Transactional(readOnly = true)
    public PageResponse<PropertyResponse> listAssigned(User manager, String search, String status, Pageable pageable) {
        List<Property> all = assignmentRepository.findAssignedPropertiesByManagerId(manager.getId());

        String kw = search != null && !search.isBlank() ? search.trim().toLowerCase(Locale.ROOT) : null;
        Property.Status statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = Property.Status.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                statusFilter = null;
            }
        }

        final Property.Status sf = statusFilter;
        List<Property> filtered = all.stream()
                .filter(p -> kw == null
                        || (p.getName() != null && p.getName().toLowerCase(Locale.ROOT).contains(kw))
                        || (p.getAddress() != null && p.getAddress().toLowerCase(Locale.ROOT).contains(kw)))
                .filter(p -> sf == null || p.getStatus() == sf)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<Property> slice = start >= filtered.size() ? List.of() : filtered.subList(start, end);
        PageImpl<Property> page = new PageImpl<>(slice, pageable, filtered.size());

        return new PageResponse<>(
                page.getContent().stream().map(PropertyResponse::fromEntity).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PropertyDetailResponse getDetail(User manager, UUID propertyId) {
        propertyAccessValidator.assertManagerAssigned(manager, propertyId);
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy property với ID: " + propertyId));
        // Two-step fetch — Hibernate cannot JOIN FETCH Property.floors + Floor.rooms (both bags)
        List<Floor> floors = floorRepository.findByPropertyIdWithRooms(propertyId);
        property.setFloors(floors);
        return PropertyDetailResponse.fromEntity(property);
    }

    @Transactional(readOnly = true)
    public PropertyResponse getById(User manager, UUID propertyId) {
        propertyAccessValidator.assertManagerAssigned(manager, propertyId);
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy property với ID: " + propertyId));
        return PropertyResponse.fromEntity(property);
    }
}
