package com.homestay.service;

import com.homestay.dto.request.UpdateProfileRequest;
import com.homestay.dto.response.UserProfileResponse;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.repository.BookingRepository;
import com.homestay.repository.RefreshTokenRepository;
import com.homestay.repository.UserRepository;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** FR-02 User Profile — get/update Self-scope. */
@ExtendWith(MockitoExtension.class)
class UserServiceProfileTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock BookingRepository bookingRepository;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                bookingRepository);
    }

    @Test
    void getMyProfile_returnsSelfFields() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setPhone("0901234567");
        user.setAvatarUrl("https://cdn.example.com/a.jpg");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UserProfileResponse resp = service.getMyProfile(user.getId());

        assertEquals(user.getId(), resp.getId());
        assertEquals(user.getFullName(), resp.getFullName());
        assertEquals(user.getEmail(), resp.getEmail());
        assertEquals("0901234567", resp.getPhone());
        assertEquals("https://cdn.example.com/a.jpg", resp.getAvatarUrl());
        assertEquals("CUSTOMER", resp.getRole());
        assertEquals("ACTIVE", resp.getStatus());
    }

    @Test
    void updateMyProfile_updatesAllowedFields_keepsEmailRoleStatus() {
        User user = TestFixtures.user(User.Role.MANAGER);
        user.setEmail("manager@test.local");
        user.setPhone("0901111111");
        user.setAvatarUrl("https://old.example.com/x.png");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("Nguyen Van B");
        req.setPhone("0902222222");
        req.setAvatarUrl("https://cdn.example.com/new.jpg");

        UserProfileResponse resp = service.updateMyProfile(user.getId(), req);

        assertEquals("Nguyen Van B", resp.getFullName());
        assertEquals("0902222222", resp.getPhone());
        assertEquals("https://cdn.example.com/new.jpg", resp.getAvatarUrl());
        assertEquals("manager@test.local", resp.getEmail());
        assertEquals("MANAGER", resp.getRole());
        assertEquals("ACTIVE", resp.getStatus());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("manager@test.local", captor.getValue().getEmail());
        assertEquals(User.Role.MANAGER, captor.getValue().getRole());
        assertEquals(User.Status.ACTIVE, captor.getValue().getStatus());
    }

    @Test
    void updateMyProfile_partial_omittedFieldsUnchanged() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setFullName("Keep Name");
        user.setPhone("0903333333");
        user.setAvatarUrl("https://keep.example.com/a.jpg");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setPhone("0904444444");

        UserProfileResponse resp = service.updateMyProfile(user.getId(), req);

        assertEquals("Keep Name", resp.getFullName());
        assertEquals("0904444444", resp.getPhone());
        assertEquals("https://keep.example.com/a.jpg", resp.getAvatarUrl());
    }

    @Test
    void updateMyProfile_blankPhoneAndAvatar_clears() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setPhone("0905555555");
        user.setAvatarUrl("https://cdn.example.com/a.jpg");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setPhone("  ");
        req.setAvatarUrl("");

        UserProfileResponse resp = service.updateMyProfile(user.getId(), req);

        assertNull(resp.getPhone());
        assertNull(resp.getAvatarUrl());
    }

    @Test
    void updateMyProfile_blankFullName_throws() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("   ");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateMyProfile(user.getId(), req));
        assertTrue(ex.getMessage().contains("Họ tên"));
    }

    @Test
    void updateMyProfile_invalidPhone_throws() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setPhone("12345");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateMyProfile(user.getId(), req));
        assertTrue(ex.getMessage().toLowerCase().contains("điện thoại")
                || ex.getMessage().toLowerCase().contains("phone"));
    }

    @Test
    void updateMyProfile_avatarMustBeHttp() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setAvatarUrl("ftp://files.example.com/a.jpg");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateMyProfile(user.getId(), req));
        assertTrue(ex.getMessage().toLowerCase().contains("http"));
    }

    @Test
    void updateMyProfile_fullNameMax255() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("A".repeat(256));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateMyProfile(user.getId(), req));
        assertTrue(ex.getMessage().contains("255"));
    }

    @Test
    void getMyProfile_unknownUser_throws() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(com.homestay.exception.ResourceNotFoundException.class,
                () -> service.getMyProfile(id));
    }
}
