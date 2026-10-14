package com.homestay.service;

import com.homestay.dto.request.ForgotPasswordRequest;
import com.homestay.dto.request.GoogleCompleteLinkRequest;
import com.homestay.dto.request.LoginRequest;
import com.homestay.dto.request.OtpVerifyRequest;
import com.homestay.dto.request.RefreshTokenRequest;
import com.homestay.dto.request.RegisterRequest;
import com.homestay.dto.request.ResendOtpRequest;
import com.homestay.dto.request.ResetPasswordRequest;
import com.homestay.dto.response.AuthResponse;
import com.homestay.entity.RefreshToken;
import com.homestay.entity.User;
import com.homestay.exception.AccountNotVerifiedException;
import com.homestay.exception.BusinessException;
import com.homestay.exception.OtpExpiredException;
import com.homestay.exception.ResourceNotFoundException;
import com.homestay.repository.RefreshTokenRepository;
import com.homestay.repository.UserRepository;
import com.homestay.security.JwtTokenProvider;
import com.homestay.support.TestFixtures;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Additional AuthService branch coverage toward ≥80% line coverage.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceCoverageTest {

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

    private void stubMail() {
        MimeMessage mime = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mime);
        doNothing().when(mailSender).send(any(MimeMessage.class));
    }

    private void stubAuthTokens(User user) {
        when(jwtTokenProvider.generateToken(user.getId())).thenReturn("access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── register ────────────────────────────────────────────────────────────

    @Test
    void register_emailExists_throwsBusinessException() {
        when(userRepository.existsByEmail("dup@test.local")).thenReturn(true);

        RegisterRequest req = new RegisterRequest();
        req.setFullName("Dup User");
        req.setEmail("dup@test.local");
        req.setPhone("0912345678");
        req.setPassword("Secret1!");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.register(req));
        assertTrue(ex.getMessage().toLowerCase().contains("email"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_newEmail_savesInactiveWithOtp() {
        stubMail();
        when(userRepository.existsByEmail("new@test.local")).thenReturn(false);
        when(passwordEncoder.encode("Secret1!")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterRequest req = new RegisterRequest();
        req.setFullName("New User");
        req.setEmail("new@test.local");
        req.setPhone("0912345678");
        req.setPassword("Secret1!");

        service.register(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals(User.Status.INACTIVE, saved.getStatus());
        assertEquals(User.Role.CUSTOMER, saved.getRole());
        assertEquals("hashed", saved.getPasswordHash());
        assertNotNull(saved.getOtpCode());
        assertNotNull(saved.getOtpExpiredAt());
    }

    @Test
    void register_smtpNotConfigured_throwsBusinessException() {
        ReflectionTestUtils.setField(service, "mailFrom", "no-reply@dev.local");
        when(userRepository.existsByEmail("new@test.local")).thenReturn(false);
        when(passwordEncoder.encode("Secret1!")).thenReturn("hashed");

        RegisterRequest req = new RegisterRequest();
        req.setFullName("New User");
        req.setEmail("new@test.local");
        req.setPhone("0912345678");
        req.setPassword("Secret1!");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.register(req));
        assertTrue(ex.getMessage().toLowerCase().contains("smtp")
                || ex.getMessage().toLowerCase().contains("email"));
    }

    // ── verifyOtp ───────────────────────────────────────────────────────────

    @Test
    void verifyOtp_unknownEmail_throwsNotFound() {
        when(userRepository.findByEmail("ghost@test.local")).thenReturn(Optional.empty());

        OtpVerifyRequest req = new OtpVerifyRequest();
        req.setEmail("ghost@test.local");
        req.setOtpCode("123456");

        assertThrows(ResourceNotFoundException.class, () -> service.verifyOtp(req));
    }

    @Test
    void verifyOtp_wrongCode_throwsBusinessException() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setStatus(User.Status.INACTIVE);
        user.setOtpCode("111111");
        user.setOtpExpiredAt(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        OtpVerifyRequest req = new OtpVerifyRequest();
        req.setEmail(user.getEmail());
        req.setOtpCode("999999");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.verifyOtp(req));
        assertTrue(ex.getMessage().toLowerCase().contains("otp"));
    }

    @Test
    void verifyOtp_expired_throwsOtpExpiredException() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setStatus(User.Status.INACTIVE);
        user.setOtpCode("123456");
        user.setOtpExpiredAt(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        OtpVerifyRequest req = new OtpVerifyRequest();
        req.setEmail(user.getEmail());
        req.setOtpCode("123456");

        assertThrows(OtpExpiredException.class, () -> service.verifyOtp(req));
    }

    @Test
    void verifyOtp_valid_activatesAndClearsOtp() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setStatus(User.Status.INACTIVE);
        user.setOtpCode("123456");
        user.setOtpExpiredAt(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        OtpVerifyRequest req = new OtpVerifyRequest();
        req.setEmail(user.getEmail());
        req.setOtpCode("123456");

        service.verifyOtp(req);

        assertEquals(User.Status.ACTIVE, user.getStatus());
        assertNull(user.getOtpCode());
        assertNull(user.getOtpExpiredAt());
        verify(userRepository).save(user);
    }

    @Test
    void verifyOtp_staffInactive_cannotSelfActivate() {
        User user = TestFixtures.user(User.Role.MANAGER);
        user.setStatus(User.Status.INACTIVE);
        user.setOtpCode("123456");
        user.setOtpExpiredAt(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        OtpVerifyRequest req = new OtpVerifyRequest();
        req.setEmail(user.getEmail());
        req.setOtpCode("123456");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.verifyOtp(req));
        assertTrue(ex.getMessage().toLowerCase().contains("admin")
                || ex.getMessage().toLowerCase().contains("kích hoạt"));
        assertEquals(User.Status.INACTIVE, user.getStatus());
        verify(userRepository, never()).save(any());
    }

    // ── resendOtp ───────────────────────────────────────────────────────────

    @Test
    void resendOtp_unknownEmail_throwsNotFound() {
        when(userRepository.findByEmail("ghost@test.local")).thenReturn(Optional.empty());

        ResendOtpRequest req = new ResendOtpRequest();
        req.setEmail("ghost@test.local");

        assertThrows(ResourceNotFoundException.class, () -> service.resendOtp(req));
    }

    @Test
    void resendOtp_alreadyActive_throwsBusinessException() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setStatus(User.Status.ACTIVE);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        ResendOtpRequest req = new ResendOtpRequest();
        req.setEmail(user.getEmail());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.resendOtp(req));
        assertTrue(ex.getMessage().toLowerCase().contains("kích hoạt"));
    }

    @Test
    void resendOtp_inactive_issuesNewOtp() {
        stubMail();
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setStatus(User.Status.INACTIVE);
        user.setOtpSendWindowStart(LocalDateTime.now().minusHours(2));
        user.setOtpSendCount(3);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ResendOtpRequest req = new ResendOtpRequest();
        req.setEmail(user.getEmail());

        service.resendOtp(req);

        assertNotNull(user.getOtpCode());
        assertEquals(1, user.getOtpSendCount());
        verify(userRepository).save(user);
    }

    // ── login ───────────────────────────────────────────────────────────────

    @Test
    void login_unknownEmail_throwsBusinessException() {
        when(userRepository.findByEmail("ghost@test.local")).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setEmail("ghost@test.local");
        req.setPassword("Secret1!");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.login(req));
        assertTrue(ex.getMessage().toLowerCase().contains("email")
                || ex.getMessage().toLowerCase().contains("mật khẩu"));
    }

    @Test
    void login_googleOnlyAccount_throwsBusinessException() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setPasswordHash(null);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        LoginRequest req = new LoginRequest();
        req.setEmail(user.getEmail());
        req.setPassword("Secret1!");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.login(req));
        assertTrue(ex.getMessage().toLowerCase().contains("google"));
    }

    @Test
    void login_wrongPassword_throwsBusinessException() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setPasswordHash("hash");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Wrong1!", "hash")).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setEmail(user.getEmail());
        req.setPassword("Wrong1!");

        assertThrows(BusinessException.class, () -> service.login(req));
    }

    @Test
    void login_inactive_throwsAccountNotVerified() {
        stubMail();
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setPasswordHash("hash");
        user.setStatus(User.Status.INACTIVE);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret1!", "hash")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        LoginRequest req = new LoginRequest();
        req.setEmail(user.getEmail());
        req.setPassword("Secret1!");

        assertThrows(AccountNotVerifiedException.class, () -> service.login(req));
        assertNotNull(user.getOtpCode());
    }

    // ── Google resolve / complete ───────────────────────────────────────────

    @Test
    void resolveGoogleSignIn_existingGoogleId_returnsTokens() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setGoogleId("g-existing");
        when(userRepository.findByGoogleId("g-existing")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        stubAuthTokens(user);

        AuthResponse resp = service.resolveGoogleSignIn("g-existing", user.getEmail(), "Name", null);

        assertFalse(resp.isPendingVerification());
        assertEquals("access-token", resp.getAccessToken());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resolveGoogleSignIn_emailSuspended_throwsBusinessException() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setStatus(User.Status.SUSPENDED);
        user.setGoogleId(null);
        when(userRepository.findByGoogleId("g-sus")).thenReturn(Optional.empty());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.resolveGoogleSignIn("g-sus", user.getEmail(), "Name", null));
        assertTrue(ex.getMessage().toLowerCase().contains("tạm khóa"));
    }

    @Test
    void resolveGoogleSignIn_activeLocal_setsAvatarWhenMissing() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setStatus(User.Status.ACTIVE);
        user.setGoogleId(null);
        user.setAvatarUrl(null);
        when(userRepository.findByGoogleId("g-av")).thenReturn(Optional.empty());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        stubAuthTokens(user);

        AuthResponse resp = service.resolveGoogleSignIn("g-av", user.getEmail(), "Name", "https://img/a.png");

        assertEquals("g-av", user.getGoogleId());
        assertEquals("https://img/a.png", user.getAvatarUrl());
        assertEquals("access-token", resp.getAccessToken());
    }

    @Test
    void resolveGoogleSignIn_newUser_nullFullName_usesEmail() {
        when(userRepository.findByGoogleId("g-nf")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("nf@test.local")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) {
                ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            }
            return u;
        });
        when(jwtTokenProvider.generateToken(any())).thenReturn("access-nf");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse resp = service.resolveGoogleSignIn("g-nf", "nf@test.local", null, null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("nf@test.local", captor.getValue().getFullName());
        assertEquals("access-nf", resp.getAccessToken());
    }

    @Test
    void completeGoogleLink_invalidToken_throwsBusinessException() {
        when(jwtTokenProvider.parseGoogleLinkToken("bad"))
                .thenThrow(new JwtException("expired"));

        GoogleCompleteLinkRequest req = new GoogleCompleteLinkRequest();
        req.setVerificationToken("bad");
        req.setOtpCode("123456");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.completeGoogleLink(req));
        assertTrue(ex.getMessage().toLowerCase().contains("google")
                || ex.getMessage().toLowerCase().contains("hết hạn"));
    }

    @Test
    void completeGoogleLink_unknownEmail_throwsNotFound() {
        Claims claims = mock(Claims.class);
        when(claims.get("email", String.class)).thenReturn("ghost@test.local");
        when(claims.get("googleId", String.class)).thenReturn("g-x");
        when(claims.get("fullName", String.class)).thenReturn("X");
        when(claims.get("avatar", String.class)).thenReturn(null);
        when(jwtTokenProvider.parseGoogleLinkToken("vt")).thenReturn(claims);
        when(userRepository.findByEmail("ghost@test.local")).thenReturn(Optional.empty());

        GoogleCompleteLinkRequest req = new GoogleCompleteLinkRequest();
        req.setVerificationToken("vt");
        req.setOtpCode("123456");

        assertThrows(ResourceNotFoundException.class, () -> service.completeGoogleLink(req));
    }

    @Test
    void completeGoogleLink_suspended_throwsBusinessException() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setStatus(User.Status.SUSPENDED);
        Claims claims = mock(Claims.class);
        when(claims.get("email", String.class)).thenReturn(user.getEmail());
        when(claims.get("googleId", String.class)).thenReturn("g-s");
        when(claims.get("fullName", String.class)).thenReturn("S");
        when(claims.get("avatar", String.class)).thenReturn(null);
        when(jwtTokenProvider.parseGoogleLinkToken("vt")).thenReturn(claims);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        GoogleCompleteLinkRequest req = new GoogleCompleteLinkRequest();
        req.setVerificationToken("vt");
        req.setOtpCode("123456");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.completeGoogleLink(req));
        assertTrue(ex.getMessage().toLowerCase().contains("tạm khóa"));
    }

    @Test
    void completeGoogleLink_setsAvatarAndBlankFullName() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setStatus(User.Status.INACTIVE);
        user.setOtpCode("123456");
        user.setOtpExpiredAt(LocalDateTime.now().plusMinutes(10));
        user.setAvatarUrl(null);
        user.setFullName("   ");

        Claims claims = mock(Claims.class);
        when(claims.get("email", String.class)).thenReturn(user.getEmail());
        when(claims.get("googleId", String.class)).thenReturn("g-fill");
        when(claims.get("fullName", String.class)).thenReturn("From Google");
        when(claims.get("avatar", String.class)).thenReturn("https://img/g.png");
        when(jwtTokenProvider.parseGoogleLinkToken("vt")).thenReturn(claims);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        stubAuthTokens(user);

        GoogleCompleteLinkRequest req = new GoogleCompleteLinkRequest();
        req.setVerificationToken("vt");
        req.setOtpCode("123456");

        AuthResponse resp = service.completeGoogleLink(req);

        assertEquals("g-fill", user.getGoogleId());
        assertEquals("https://img/g.png", user.getAvatarUrl());
        assertEquals("From Google", user.getFullName());
        assertEquals(User.Status.ACTIVE, user.getStatus());
        assertEquals("access-token", resp.getAccessToken());
    }

    // ── forgot / reset password ─────────────────────────────────────────────

    @Test
    void forgotPassword_localAccount_sendsOtp() {
        stubMail();
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setPasswordHash("hash");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail(user.getEmail());

        service.forgotPassword(req);

        assertNotNull(user.getOtpCode());
        verify(userRepository).save(user);
    }

    @Test
    void resetPassword_unknownEmail_genericOtpError_noDisclosure() {
        when(userRepository.findByEmail("ghost@test.local")).thenReturn(Optional.empty());

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("ghost@test.local");
        req.setOtpCode("123456");
        req.setNewPassword("NewSecret1!");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.resetPassword(req));
        assertTrue(ex.getMessage().toLowerCase().contains("otp"));
        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).revokeAllByUser(any());
    }

    @Test
    void resetPassword_valid_updatesHashAndRevokesTokens() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setPasswordHash("old-hash");
        user.setOtpCode("123456");
        user.setOtpExpiredAt(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewSecret1!")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail(user.getEmail());
        req.setOtpCode("123456");
        req.setNewPassword("NewSecret1!");

        service.resetPassword(req);

        assertEquals("new-hash", user.getPasswordHash());
        assertNull(user.getOtpCode());
        verify(refreshTokenRepository).revokeAllByUser(user);
    }

    // ── refresh / logout ────────────────────────────────────────────────────

    @Test
    void refreshToken_unknown_throwsBusinessException() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("missing");

        assertThrows(BusinessException.class, () -> service.refreshToken(req));
    }

    @Test
    void refreshToken_expired_throwsBusinessException() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(refreshTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken(token.getToken());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.refreshToken(req));
        assertTrue(ex.getMessage().toLowerCase().contains("hết hạn")
                || ex.getMessage().toLowerCase().contains("thu hồi"));
    }

    @Test
    void refreshToken_valid_rotatesAndReturnsNewTokens() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        RefreshToken old = new RefreshToken();
        old.setToken(UUID.randomUUID().toString());
        old.setUser(user);
        old.setExpiresAt(LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByToken(old.getToken())).thenReturn(Optional.of(old));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenProvider.generateToken(user.getId())).thenReturn("new-access");

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken(old.getToken());

        AuthResponse resp = service.refreshToken(req);

        assertEquals("new-access", resp.getAccessToken());
        assertNotNull(resp.getRefreshToken());
        assertNotNull(old.getRevokedAt());
    }

    @Test
    void logout_knownToken_revokes() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken(token.getToken());

        service.logout(req);

        assertNotNull(token.getRevokedAt());
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void logout_unknownToken_noException() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("missing");

        assertDoesNotThrow(() -> service.logout(req));
        verify(refreshTokenRepository, never()).save(any());
    }

    // ── mail helpers ────────────────────────────────────────────────────────

    @Test
    void issueOtp_mailSendFails_throwsBusinessException() {
        MimeMessage mime = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mime);
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setStatus(User.Status.INACTIVE);
        user.setPasswordHash("hash");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret1!", "hash")).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setEmail(user.getEmail());
        req.setPassword("Secret1!");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.login(req));
        assertTrue(ex.getMessage().toLowerCase().contains("otp")
                || ex.getMessage().toLowerCase().contains("email"));
    }

    @Test
    void register_blankMailFrom_fallsBackToJavaMailSenderImplUsername() {
        JavaMailSenderImpl impl = mock(JavaMailSenderImpl.class);
        MimeMessage mime = new MimeMessage(Session.getInstance(new Properties()));
        when(impl.getUsername()).thenReturn("smtp-user@gmail.com");
        when(impl.createMimeMessage()).thenReturn(mime);
        doNothing().when(impl).send(any(MimeMessage.class));

        service = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtTokenProvider,
                impl);
        ReflectionTestUtils.setField(service, "googleClientId", "test-google-client-id");
        ReflectionTestUtils.setField(service, "mailFrom", "");
        ReflectionTestUtils.setField(service, "refreshExpirationDays", 7L);

        when(userRepository.existsByEmail("fb@test.local")).thenReturn(false);
        when(passwordEncoder.encode("Secret1!")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterRequest req = new RegisterRequest();
        req.setFullName("Fallback");
        req.setEmail("fb@test.local");
        req.setPhone("0912345678");
        req.setPassword("Secret1!");

        service.register(req);

        verify(impl).send(any(MimeMessage.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void loginWithGoogle_invalidToken_throwsBusinessException() {
        com.homestay.dto.request.GoogleAuthRequest req =
                new com.homestay.dto.request.GoogleAuthRequest();
        req.setIdToken("not-a-real-google-token");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.loginWithGoogle(req));
        assertTrue(ex.getMessage().toLowerCase().contains("google"));
    }
}
