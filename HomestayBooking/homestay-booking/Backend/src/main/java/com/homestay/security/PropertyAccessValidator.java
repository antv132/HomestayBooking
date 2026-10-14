package com.homestay.security;

import com.homestay.entity.ManagerPropertyAssignment;
import com.homestay.entity.User;
import com.homestay.exception.ForbiddenException;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** FR-06 — Manager may only access properties with an ACTIVE assignment. */
@Component
@RequiredArgsConstructor
public class PropertyAccessValidator {

    private final ManagerPropertyAssignmentRepository assignmentRepository;

    /**
     * FR-05 T053 / FR-004: Admin = all properties; Manager = ACTIVE assignment only.
     */
    public void assertManagerAssigned(User manager, UUID propertyId) {
        if (manager == null || propertyId == null) {
            throw new ForbiddenException("Bạn không có quyền truy cập property này");
        }
        if (manager.getRole() == User.Role.ADMIN) {
            return;
        }
        boolean assigned = assignmentRepository.existsByManagerIdAndPropertyIdAndStatus(
                manager.getId(), propertyId, ManagerPropertyAssignment.Status.ACTIVE);
        if (!assigned) {
            throw new ForbiddenException("Bạn không có quyền truy cập property này");
        }
    }
}
