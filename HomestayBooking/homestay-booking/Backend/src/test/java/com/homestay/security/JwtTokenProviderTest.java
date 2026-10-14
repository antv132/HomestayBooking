package com.homestay.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;
    private static final String TEST_SECRET = "test-secret-key-that-is-long-enough-for-hs256-algorithm-minimum-256-bits";

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 3600000L);
    }

    @Test
    void generateToken_success() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateToken(userId);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    void getUserIdFromToken_success() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateToken(userId);

        UUID extracted = provider.getUserIdFromToken(token);

        assertEquals(userId, extracted);
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateToken(userId);

        assertTrue(provider.validateToken(token));
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        assertFalse(provider.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_emptyToken_returnsFalse() {
        assertFalse(provider.validateToken(""));
    }

    @Test
    void validateToken_nullToken_returnsFalse() {
        assertFalse(provider.validateToken(null));
    }

    @Test
    void validateToken_tampereredToken_returnsFalse() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateToken(userId);
        String tampered = token.substring(0, token.length() - 10) + "tamperedxx";

        assertFalse(provider.validateToken(tampered));
    }

    @Test
    void getUserIdFromToken_invalidToken_throwsException() {
        assertThrows(Exception.class, () -> provider.getUserIdFromToken("invalid.token"));
    }

    @Test
    void generateGoogleLinkToken_success() {
        String token = provider.generateGoogleLinkToken(
                "google123",
                "user@gmail.com",
                "Test User",
                "https://example.com/avatar.jpg"
        );

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void parseGoogleLinkToken_success() {
        String token = provider.generateGoogleLinkToken(
                "google123",
                "user@gmail.com",
                "Test User",
                "https://example.com/avatar.jpg"
        );

        Claims claims = provider.parseGoogleLinkToken(token);

        assertNotNull(claims);
        assertEquals("user@gmail.com", claims.getSubject());
        assertEquals(JwtTokenProvider.TYPE_GOOGLE_LINK, claims.get(JwtTokenProvider.CLAIM_TYPE));
        assertEquals("google123", claims.get("googleId"));
        assertEquals("user@gmail.com", claims.get("email"));
        assertEquals("Test User", claims.get("fullName"));
        assertEquals("https://example.com/avatar.jpg", claims.get("avatar"));
    }

    @Test
    void parseGoogleLinkToken_wrongType_throwsJwtException() {
        UUID userId = UUID.randomUUID();
        String regularToken = provider.generateToken(userId);

        assertThrows(JwtException.class, () -> provider.parseGoogleLinkToken(regularToken));
    }

    @Test
    void parseGoogleLinkToken_invalidToken_throwsException() {
        assertThrows(Exception.class, () -> provider.parseGoogleLinkToken("invalid.token"));
    }

    @Test
    void generateGoogleLinkToken_withNullAvatar_success() {
        String token = provider.generateGoogleLinkToken(
                "google456",
                "test@gmail.com",
                "Another User",
                null
        );

        Claims claims = provider.parseGoogleLinkToken(token);
        assertNotNull(claims);
        assertNull(claims.get("avatar"));
    }

    @Test
    void generateToken_differentUsers_produceDifferentTokens() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        String token1 = provider.generateToken(user1);
        String token2 = provider.generateToken(user2);

        assertNotEquals(token1, token2);
    }

    @Test
    void googleLinkToken_hasCorrectTtl() {
        assertEquals(900000L, JwtTokenProvider.GOOGLE_LINK_TTL_MS);
    }
}
