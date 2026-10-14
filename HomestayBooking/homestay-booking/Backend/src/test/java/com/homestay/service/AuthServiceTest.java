package com.homestay.service;

import com.homestay.dto.request.LoginRequest;
import com.homestay.dto.request.RefreshTokenRequest;
import com.homestay.dto.response.AuthResponse;
import com.homestay.entity.RefreshToken;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.repository.RefreshTokenRepository;
import com.homestay.repository.UserRepository;
import com.homestay.security.JwtTokenProvider;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock JavaMailSender mailSender;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtTokenProvider,
                mailSender);
        ReflectionTestUtils.setField(service, "googleClientId", "test-google-client-id");
        ReflectionTestUtils.setField(service, "mailFrom", "noreply@test.local");
        ReflectionTestUtils.setField(service, "refreshExpirationDays", 7L);
    }

    @Test
    void login_suspendedUser_denied() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setPasswordHash("hash");
        user.setStatus(User.Status.SUSPENDED);
        when(userRepository.findByEmail("suspended@test.local")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret1!", "hash")).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setEmail("suspended@test.local");
        req.setPassword("Secret1!");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.login(req));
        assertTrue(ex.getMessage().toLowerCase().contains("tạm khóa")
                || ex.getMessage().toLowerCase().contains("suspend"));
    }

    @Test
    void login_activeUser_returnsTokens() {
        User user = TestFixtures.user(User.Role.MANAGER);
        user.setPasswordHash("hash");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret1!", "hash")).thenReturn(true);
        when(jwtTokenProvider.generateToken(user.getId())).thenReturn("access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        LoginRequest req = new LoginRequest();
        req.setEmail(user.getEmail());
        req.setPassword("Secret1!");

        AuthResponse resp = service.login(req);

        assertEquals("access-token", resp.getAccessToken());
        assertNotNull(resp.getRefreshToken());
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertEquals(user, captor.getValue().getUser());
        LocalDateTime expiresAt = captor.getValue().getExpiresAt();
        assertNotNull(expiresAt);
        assertTrue(expiresAt.isAfter(LocalDateTime.now().plusDays(6)));
        assertTrue(expiresAt.isBefore(LocalDateTime.now().plusDays(8)));
    }

    @Test
    void refreshToken_revoked_denied() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusDays(1));
        token.setRevokedAt(LocalDateTime.now());
        when(refreshTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken(token.getToken());

        assertThrows(BusinessException.class, () -> service.refreshToken(req));
    }
}
