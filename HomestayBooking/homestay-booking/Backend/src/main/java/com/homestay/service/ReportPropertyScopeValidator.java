package com.homestay.service;

import com.homestay.entity.ManagerPropertyAssignment;
import com.homestay.entity.User;
import com.homestay.exception.ForbiddenException;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Validates Manager access to a Property via ACTIVE assignment (FR-06 / FR-16 / FR-20). */
@Component
@RequiredArgsConstructor
public class ReportPropertyScopeValidator {

    private final ManagerPropertyAssignmentRepository assignmentRepository;

    /**
     * Manager must have ACTIVE assignment. Admin bypasses property scope (FR-20).
     */
    public void validateManagerAccess(User actor, UUID propertyId) {
        if (actor == null || propertyId == null) {
            throw new ForbiddenException("Bạn không có quyền truy cập property này");
        }
        if (actor.getRole() == User.Role.ADMIN) {
            return;
        }
        boolean assigned = assignmentRepository.existsByManagerIdAndPropertyIdAndStatus(
                actor.getId(), propertyId, ManagerPropertyAssignment.Status.ACTIVE);
        if (!assigned) {
            throw new ForbiddenException("Bạn không có quyền truy cập property này");
        }
    }
}
