package com.homestay.service;

import com.homestay.dto.request.ForgotPasswordRequest;
import com.homestay.dto.request.OtpVerifyRequest;
import com.homestay.dto.request.RefreshTokenRequest;
import com.homestay.dto.request.ResetPasswordRequest;
import com.homestay.entity.RefreshToken;
import com.homestay.entity.User;
import com.homestay.exception.AccountNotVerifiedException;
import com.homestay.exception.BusinessException;
import com.homestay.repository.RefreshTokenRepository;
import com.homestay.repository.UserRepository;
import com.homestay.security.JwtTokenProvider;
import com.homestay.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1-A: refresh TTL gate + forgot-password anti-enumeration (001 FR-007/008/010).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceP1aTest {

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
    void refreshToken_suspended_denied() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setStatus(User.Status.SUSPENDED);
        RefreshToken token = validToken(user);
        when(refreshTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken(token.getToken());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.refreshToken(req));
        assertTrue(ex.getMessage().toLowerCase().contains("tạm khóa")
                || ex.getMessage().toLowerCase().contains("suspend"));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refreshToken_inactive_denied() throws Exception {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setStatus(User.Status.INACTIVE);
        user.setEmail("inactive@test.local");
        RefreshToken token = validToken(user);
        when(refreshTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        jakarta.mail.internet.MimeMessage mime = org.mockito.Mockito.mock(jakarta.mail.internet.MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mime);

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken(token.getToken());

        assertThrows(AccountNotVerifiedException.class, () -> service.refreshToken(req));
    }

    @Test
    void forgotPassword_unknownEmail_noException_noSideEffects() {
        when(userRepository.findByEmail("ghost@test.local")).thenReturn(Optional.empty());

        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("ghost@test.local");

        assertDoesNotThrow(() -> service.forgotPassword(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void forgotPassword_googleOnly_noException_noOtp() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setPasswordHash(null);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail(user.getEmail());

        assertDoesNotThrow(() -> service.forgotPassword(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_unknownEmail_genericOtpError() {
        when(userRepository.findByEmail("ghost@test.local")).thenReturn(Optional.empty());

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("ghost@test.local");
        req.setOtpCode("123456");
        req.setNewPassword("NewSecret1!");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.resetPassword(req));
        assertTrue(ex.getMessage().toLowerCase().contains("otp"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyOtp_staffInactive_blocked() {
        User user = TestFixtures.user(User.Role.EMPLOYEE);
        user.setStatus(User.Status.INACTIVE);
        user.setOtpCode("123456");
        user.setOtpExpiredAt(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        OtpVerifyRequest req = new OtpVerifyRequest();
        req.setEmail(user.getEmail());
        req.setOtpCode("123456");

        assertThrows(BusinessException.class, () -> service.verifyOtp(req));
        assertEquals(User.Status.INACTIVE, user.getStatus());
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyOtp_customerInactive_activates() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setStatus(User.Status.INACTIVE);
        user.setOtpCode("123456");
        user.setOtpExpiredAt(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        OtpVerifyRequest req = new OtpVerifyRequest();
        req.setEmail(user.getEmail());
        req.setOtpCode("123456");

        assertDoesNotThrow(() -> service.verifyOtp(req));
        assertEquals(User.Status.ACTIVE, user.getStatus());
    }

    private static RefreshToken validToken(User user) {
        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusDays(7));
        return token;
    }
}
