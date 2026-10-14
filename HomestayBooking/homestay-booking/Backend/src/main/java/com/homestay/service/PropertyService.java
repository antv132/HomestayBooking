package com.homestay.service;

import com.homestay.dto.response.FeaturedPropertyResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.dto.response.PropertyResponse;
import com.homestay.entity.Property;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.PropertyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;

    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Transactional(readOnly = true)
    public List<FeaturedPropertyResponse> getFeatured(int limit) {
        PageRequest pageable = PageRequest.of(0, Math.min(limit, 20), Sort.by("createdAt").descending());
        return propertyRepository.findByStatus(Property.Status.ACTIVE, pageable)
                .getContent().stream()
                .map(FeaturedPropertyResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Guest / public list — defaults to ACTIVE when status omitted (FR-06 / FR-03).
     * Explicit status=INACTIVE is ignored for public (still ACTIVE-only).
     */
    @Transactional(readOnly = true)
    public PageResponse<PropertyResponse> getAllPublic(String search, String status, Pageable pageable) {
        Property.Status effective = Property.Status.ACTIVE;
        if (status != null && !status.isBlank()) {
            try {
                Property.Status requested = Property.Status.valueOf(status.trim().toUpperCase());
                // Public discovery never exposes INACTIVE
                if (requested == Property.Status.ACTIVE) {
                    effective = Property.Status.ACTIVE;
                }
            } catch (IllegalArgumentException ignored) {
                // keep ACTIVE
            }
        }

        Page<Property> page;
        if (search != null && !search.isBlank()) {
            page = propertyRepository.searchByNameOrAddressAndStatus(search.trim(), effective, pageable);
        } else {
            page = propertyRepository.findByStatus(effective, pageable);
        }

        return new PageResponse<>(
                page.getContent().stream().map(PropertyResponse::fromEntity).collect(Collectors.toList()),
                page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    /** Public detail — ACTIVE only. */
    @Transactional(readOnly = true)
    public PropertyResponse getPublicById(UUID id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy property với ID: " + id));
        if (property.getStatus() != Property.Status.ACTIVE) {
            throw new ResourceNotFoundException("Không tìm thấy property với ID: " + id);
        }
        return PropertyResponse.fromEntity(property);
    }
}
