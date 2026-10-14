package com.homestay.service;

import com.homestay.dto.request.AdminCreatePropertyRequest;
import com.homestay.dto.request.AdminUpdatePropertyRequest;
import com.homestay.dto.response.AdminPropertyResponse;
import com.homestay.dto.response.ManagerAssignmentResponse;
import com.homestay.dto.response.PageResponse;
import com.homestay.entity.ManagerPropertyAssignment;
import com.homestay.entity.Property;
import com.homestay.entity.User;
import com.homestay.exception.ConflictException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.repository.PropertyRepository;
import com.homestay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FR-06 — Admin Property CRUD + Manager assignment (SCR-46–49).
 */
@Service
@RequiredArgsConstructor
public class AdminPropertyService {

    public static final String PROPERTY_CREATED = "PROPERTY_CREATED";
    public static final String PROPERTY_UPDATED = "PROPERTY_UPDATED";
    public static final String PROPERTY_STATUS_CHANGED = "PROPERTY_STATUS_CHANGED";
    public static final String MANAGER_ASSIGNED = "MANAGER_ASSIGNED";
    public static final String MANAGER_ASSIGNMENT_ENDED = "MANAGER_ASSIGNMENT_ENDED";

    private final PropertyRepository propertyRepository;
    private final ManagerPropertyAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public PageResponse<AdminPropertyResponse> listProperties(
            String keyword, String status, UUID managerId, Pageable pageable) {
        Property.Status statusFilter = parseStatus(status);
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        var page = propertyRepository.searchForAdmin(kw, statusFilter, managerId, pageable);
        List<Property> properties = page.getContent();

        Map<UUID, User> managerByPropertyId = Map.of();
        if (!properties.isEmpty()) {
            List<UUID> propertyIds = properties.stream().map(Property::getId).toList();
            managerByPropertyId = assignmentRepository
                    .findActiveByPropertyIds(propertyIds, ManagerPropertyAssignment.Status.ACTIVE)
                    .stream()
                    .collect(Collectors.toMap(
                            mpa -> mpa.getProperty().getId(),
                            ManagerPropertyAssignment::getManager,
                            (existing, ignored) -> existing));
        }

        final Map<UUID, User> managers = managerByPropertyId;
        List<AdminPropertyResponse> content = properties.stream()
                .map(p -> AdminPropertyResponse.fromEntity(p, managers.get(p.getId())))
                .toList();

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional
    public AdminPropertyResponse createProperty(AdminCreatePropertyRequest req, User actor) {
        Property property = new Property();
        property.setName(req.getName().trim());
        property.setAddress(req.getAddress().trim());

        // FR-06: default INACTIVE until manager assigned / explicitly activated with gate
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            Property.Status status = parseStatus(req.getStatus());
            if (status == Property.Status.ACTIVE) {
                throw new ConflictException("Assign a manager before activating");
            }
            property.setStatus(status != null ? status : Property.Status.INACTIVE);
        } else {
            property.setStatus(Property.Status.INACTIVE);
        }

        property.setDescription(
                req.getDescription() != null && !req.getDescription().isBlank()
                        ? req.getDescription().trim()
                        : null);

        Property saved = propertyRepository.save(property);
        activityLogService.log(actor, PROPERTY_CREATED, "Property", saved.getId(),
                "Created property: " + saved.getName());
        return AdminPropertyResponse.fromEntity(saved, null);
    }

    @Transactional(readOnly = true)
    public AdminPropertyResponse getProperty(UUID id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy property với ID: " + id));

        User manager = assignmentRepository
                .findActiveByPropertyIds(List.of(property.getId()), ManagerPropertyAssignment.Status.ACTIVE)
                .stream()
                .findFirst()
                .map(ManagerPropertyAssignment::getManager)
                .orElse(null);

        return AdminPropertyResponse.fromEntity(property, manager);
    }

    @Transactional
    public AdminPropertyResponse updateProperty(UUID id, AdminUpdatePropertyRequest req, User actor) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy property với ID: " + id));

        Property.Status previousStatus = property.getStatus();

        if (req.getName() != null && !req.getName().isBlank()) {
            property.setName(req.getName().trim());
        }
        if (req.getAddress() != null && !req.getAddress().isBlank()) {
            property.setAddress(req.getAddress().trim());
        }
        if (req.getDescription() != null) {
            property.setDescription(
                    req.getDescription().isBlank() ? null : req.getDescription().trim());
        }
        if (req.getStatus() != null) {
            Property.Status status = parseStatus(req.getStatus());
            if (status != null) {
                assertCanActivate(property.getId(), status);
                property.setStatus(status);
            }
        }

        Property saved = propertyRepository.save(property);

        if (req.getStatus() != null && saved.getStatus() != previousStatus) {
            activityLogService.log(actor, PROPERTY_STATUS_CHANGED, "Property", saved.getId(),
                    previousStatus + " → " + saved.getStatus());
        } else {
            activityLogService.log(actor, PROPERTY_UPDATED, "Property", saved.getId(),
                    "Updated property: " + saved.getName());
        }

        User manager = assignmentRepository
                .findActiveByPropertyIds(List.of(saved.getId()), ManagerPropertyAssignment.Status.ACTIVE)
                .stream()
                .findFirst()
                .map(ManagerPropertyAssignment::getManager)
                .orElse(null);

        return AdminPropertyResponse.fromEntity(saved, manager);
    }

    @Transactional
    public AdminPropertyResponse assignManager(UUID propertyId, UUID managerId, User currentUser) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy property với ID: " + propertyId));

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với ID: " + managerId));

        if (manager.getRole() != User.Role.MANAGER) {
            throw new ConflictException("User được chọn không phải MANAGER");
        }
        if (manager.getStatus() != User.Status.ACTIVE) {
            throw new ConflictException("Chỉ được gán Manager đang ACTIVE");
        }

        List<ManagerPropertyAssignment> currentActive = assignmentRepository
                .findActiveByPropertyIds(List.of(propertyId), ManagerPropertyAssignment.Status.ACTIVE);
        if (!currentActive.isEmpty()) {
            currentActive.forEach(mpa -> {
                mpa.setStatus(ManagerPropertyAssignment.Status.INACTIVE);
                activityLogService.log(currentUser, MANAGER_ASSIGNMENT_ENDED, "ManagerPropertyAssignment",
                        mpa.getId(), "Ended assignment for property " + propertyId);
            });
            assignmentRepository.saveAll(currentActive);
        }

        ManagerPropertyAssignment assignment = new ManagerPropertyAssignment();
        assignment.setManager(manager);
        assignment.setProperty(property);
        assignment.setAssignedBy(currentUser);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setStatus(ManagerPropertyAssignment.Status.ACTIVE);
        assignmentRepository.save(assignment);

        activityLogService.log(currentUser, MANAGER_ASSIGNED, "ManagerPropertyAssignment",
                assignment.getId(), "Assigned manager " + manager.getEmail() + " to property " + propertyId);

        return AdminPropertyResponse.fromEntity(property, manager);
    }

    @Transactional(readOnly = true)
    public PageResponse<ManagerAssignmentResponse> listAssignmentHistory(UUID propertyId, Pageable pageable) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new ResourceNotFoundException("Không tìm thấy property với ID: " + propertyId);
        }
        List<ManagerAssignmentResponse> all = assignmentRepository.findHistoryByPropertyId(propertyId).stream()
                .map(ManagerAssignmentResponse::fromEntity)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<ManagerAssignmentResponse> slice = start >= all.size() ? List.of() : all.subList(start, end);
        int totalPages = pageable.getPageSize() == 0 ? 0
                : (int) Math.ceil((double) all.size() / pageable.getPageSize());

        return new PageResponse<>(slice, pageable.getPageNumber(), pageable.getPageSize(), all.size(), totalPages);
    }

    /** FR-008 / US4 — ACTIVE requires an ACTIVE manager assignment. */
    private void assertCanActivate(UUID propertyId, Property.Status status) {
        if (status != Property.Status.ACTIVE) {
            return;
        }
        boolean hasManager = assignmentRepository.existsByPropertyIdAndStatus(
                propertyId, ManagerPropertyAssignment.Status.ACTIVE);
        if (!hasManager) {
            throw new ConflictException("Assign a manager before activating");
        }
    }

    private Property.Status parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return Property.Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
