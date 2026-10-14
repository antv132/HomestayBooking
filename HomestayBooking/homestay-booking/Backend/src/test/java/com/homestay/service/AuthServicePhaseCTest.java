package com.homestay.service;

import com.homestay.dto.request.GoogleCompleteLinkRequest;
import com.homestay.dto.request.ResendOtpRequest;
import com.homestay.dto.response.AuthResponse;
import com.homestay.entity.RefreshToken;
import com.homestay.entity.User;
import com.homestay.exception.BusinessException;
import com.homestay.repository.RefreshTokenRepository;
import com.homestay.repository.UserRepository;
import com.homestay.security.JwtTokenProvider;
import com.homestay.support.TestFixtures;
import io.jsonwebtoken.Claims;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
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
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServicePhaseCTest {

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
    }

    private void stubMail() {
        MimeMessage mime = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mime);
        doNothing().when(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void resolveGoogleSignIn_activeLocal_autoLinks() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setStatus(User.Status.ACTIVE);
        user.setGoogleId(null);
        when(userRepository.findByGoogleId("g-1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenProvider.generateToken(user.getId())).thenReturn("access");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse resp = service.resolveGoogleSignIn("g-1", user.getEmail(), "Name", null);

        assertFalse(resp.isPendingVerification());
        assertEquals("g-1", user.getGoogleId());
        assertEquals("access", resp.getAccessToken());
    }

    @Test
    void resolveGoogleSignIn_inactiveLocal_requiresOtpLink() {
        stubMail();
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setStatus(User.Status.INACTIVE);
        user.setGoogleId(null);
        when(userRepository.findByGoogleId("g-2")).thenReturn(Optional.empty());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenProvider.generateGoogleLinkToken("g-2", user.getEmail(), "Name", null))
                .thenReturn("pending-token");

        AuthResponse resp = service.resolveGoogleSignIn("g-2", user.getEmail(), "Name", null);

        assertTrue(resp.isPendingVerification());
        assertEquals("pending-token", resp.getVerificationToken());
        assertNull(user.getGoogleId());
        assertNotNull(user.getOtpCode());
        assertTrue(user.getOtpExpiredAt().isAfter(LocalDateTime.now().plusMinutes(14)));
        assertEquals(1, user.getOtpSendCount());
    }

    @Test
    void completeGoogleLink_activatesAndLinks() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setStatus(User.Status.INACTIVE);
        user.setOtpCode("123456");
        user.setOtpExpiredAt(LocalDateTime.now().plusMinutes(10));

        Claims claims = mock(Claims.class);
        when(claims.get("email", String.class)).thenReturn(user.getEmail());
        when(claims.get("googleId", String.class)).thenReturn("g-3");
        when(claims.get("fullName", String.class)).thenReturn("Linked");
        when(claims.get("avatar", String.class)).thenReturn(null);
        when(jwtTokenProvider.parseGoogleLinkToken("vt")).thenReturn(claims);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenProvider.generateToken(user.getId())).thenReturn("access-linked");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        GoogleCompleteLinkRequest req = new GoogleCompleteLinkRequest();
        req.setVerificationToken("vt");
        req.setOtpCode("123456");

        AuthResponse resp = service.completeGoogleLink(req);

        assertEquals("g-3", user.getGoogleId());
        assertEquals(User.Status.ACTIVE, user.getStatus());
        assertNull(user.getOtpCode());
        assertEquals("access-linked", resp.getAccessToken());
    }

    @Test
    void resendOtp_fourthSendInHour_rejected() {
        User user = TestFixtures.user(User.Role.CUSTOMER);
        user.setStatus(User.Status.INACTIVE);
        user.setOtpSendWindowStart(LocalDateTime.now());
        user.setOtpSendCount(3);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        ResendOtpRequest req = new ResendOtpRequest();
        req.setEmail(user.getEmail());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.resendOtp(req));
        assertTrue(ex.getMessage().contains("3"));
    }

    @Test
    void resolveGoogleSignIn_newUser_createsActive() {
        when(userRepository.findByGoogleId("g-new")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@test.local")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) {
                ReflectionTestUtils.setField(u, "id", java.util.UUID.randomUUID());
            }
            return u;
        });
        when(jwtTokenProvider.generateToken(any())).thenReturn("access-new");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse resp = service.resolveGoogleSignIn("g-new", "new@test.local", "New User", null);

        assertFalse(resp.isPendingVerification());
        assertEquals("access-new", resp.getAccessToken());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(User.Status.ACTIVE, captor.getValue().getStatus());
        assertEquals("g-new", captor.getValue().getGoogleId());
    }
}
