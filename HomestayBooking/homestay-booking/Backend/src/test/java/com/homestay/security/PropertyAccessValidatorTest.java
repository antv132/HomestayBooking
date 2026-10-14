package com.homestay.security;

import com.homestay.entity.User;
import com.homestay.exception.ForbiddenException;
import com.homestay.repository.ManagerPropertyAssignmentRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** FR-05 T053 — Admin bypasses property assignment. */
@ExtendWith(MockitoExtension.class)
class PropertyAccessValidatorTest {

    @Mock ManagerPropertyAssignmentRepository assignmentRepository;

    private PropertyAccessValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PropertyAccessValidator(assignmentRepository);
    }

    @Test
    void admin_bypassesAssignmentCheck() {
        User admin = TestFixtures.user(User.Role.ADMIN);
        UUID propertyId = UUID.randomUUID();

        assertDoesNotThrow(() -> validator.assertManagerAssigned(admin, propertyId));
        verify(assignmentRepository, never()).existsByManagerIdAndPropertyIdAndStatus(any(), any(), any());
    }

    @Test
    void manager_unassigned_forbidden() {
        User manager = TestFixtures.user(User.Role.MANAGER);
        UUID propertyId = UUID.randomUUID();
        when(assignmentRepository.existsByManagerIdAndPropertyIdAndStatus(
                eq(manager.getId()), eq(propertyId), any())).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> validator.assertManagerAssigned(manager, propertyId));
    }
}
