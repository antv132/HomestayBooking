package com.homestay.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testUserGettersAndSetters() {
        User user = new User();
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        user.setId(id);
        user.setFullName("John Doe");
        user.setEmail("john@example.com");
        user.setPasswordHash("hash123");
        user.setGoogleId("google123");
        user.setPhone("0123456789");
        user.setAvatarUrl("http://example.com/avatar.jpg");
        user.setRole(User.Role.ADMIN);
        user.setStatus(User.Status.ACTIVE);
        user.setOtpCode("123456");
        user.setOtpExpiredAt(now);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        assertEquals(id, user.getId());
        assertEquals("John Doe", user.getFullName());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("hash123", user.getPasswordHash());
        assertEquals("google123", user.getGoogleId());
        assertEquals("0123456789", user.getPhone());
        assertEquals("http://example.com/avatar.jpg", user.getAvatarUrl());
        assertEquals(User.Role.ADMIN, user.getRole());
        assertEquals(User.Status.ACTIVE, user.getStatus());
        assertEquals("123456", user.getOtpCode());
        assertEquals(now, user.getOtpExpiredAt());
        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getUpdatedAt());
    }

    @Test
    void testDefaultValues() {
        User user = new User();
        assertEquals(User.Role.CUSTOMER, user.getRole());
        assertEquals(User.Status.INACTIVE, user.getStatus());
    }

    @Test
    void testRoleEnum() {
        assertEquals(4, User.Role.values().length);
        assertEquals(User.Role.ADMIN, User.Role.valueOf("ADMIN"));
        assertEquals(User.Role.MANAGER, User.Role.valueOf("MANAGER"));
        assertEquals(User.Role.EMPLOYEE, User.Role.valueOf("EMPLOYEE"));
        assertEquals(User.Role.CUSTOMER, User.Role.valueOf("CUSTOMER"));
    }

    @Test
    void testStatusEnum() {
        assertEquals(3, User.Status.values().length);
        assertEquals(User.Status.INACTIVE, User.Status.valueOf("INACTIVE"));
        assertEquals(User.Status.ACTIVE, User.Status.valueOf("ACTIVE"));
        assertEquals(User.Status.SUSPENDED, User.Status.valueOf("SUSPENDED"));
    }
}
