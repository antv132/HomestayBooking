package com.homestay.service;

import com.homestay.dto.request.ChangePasswordRequest;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.RefreshTokenRepository;
import com.homestay.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServicePasswordTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock BookingRepository bookingRepository;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, refreshTokenRepository, passwordEncoder, bookingRepository);
    }

    private User user() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("test@test.com");
        u.setFullName("Test User");
        u.setPasswordHash("hashed_password");
        u.setRole(User.Role.CUSTOMER);
        u.setStatus(User.Status.ACTIVE);
        return u;
    }

    @Test
    void changePassword_success() {
        User user = user();
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("OldPass123");
        req.setNewPassword("NewPass456");
        req.setConfirmPassword("NewPass456");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123", "hashed_password")).thenReturn(true);
        when(passwordEncoder.encode("NewPass456")).thenReturn("new_hashed");

        service.changePassword(user.getId(), req);

        assertEquals("new_hashed", user.getPasswordHash());
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllByUser(user);
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsBusinessException() {
        User user = user();
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("WrongPass");
        req.setNewPassword("NewPass456");
        req.setConfirmPassword("NewPass456");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass", "hashed_password")).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.changePassword(user.getId(), req));
    }

    @Test
    void changePassword_googleUser_throwsBusinessException() {
        User user = user();
        user.setPasswordHash(null);
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("any");
        req.setNewPassword("NewPass456");
        req.setConfirmPassword("NewPass456");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class, () -> service.changePassword(user.getId(), req));
    }

    @Test
    void changePassword_passwordMismatch_throwsBusinessException() {
        User user = user();
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("OldPass123");
        req.setNewPassword("NewPass456");
        req.setConfirmPassword("DifferentPass");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123", "hashed_password")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.changePassword(user.getId(), req));
    }

    @Test
    void changePassword_userNotFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("any");
        req.setNewPassword("any");
        req.setConfirmPassword("any");

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.changePassword(id, req));
    }

    @Test
    void getMyProfile_success() {
        User user = user();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> service.getMyProfile(user.getId()));
        verify(userRepository).findById(user.getId());
    }

    @Test
    void getMyProfile_userNotFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getMyProfile(id));
    }
}
