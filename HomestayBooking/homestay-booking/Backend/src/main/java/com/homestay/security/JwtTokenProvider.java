package com.homestay.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// Tạo và xác thực JWT token
@Component
public class JwtTokenProvider {

    public static final String CLAIM_TYPE = "typ";
    public static final String TYPE_GOOGLE_LINK = "GOOGLE_LINK";
    public static final long GOOGLE_LINK_TTL_MS = TimeUnit.MINUTES.toMillis(15);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // Tạo access token từ userId
    public String generateToken(UUID userId) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** Short-lived token for FR-006 Google → local OTP link (not an access token). */
    public String generateGoogleLinkToken(String googleId, String email, String fullName, String avatarUrl) {
        return Jwts.builder()
                .setSubject(email)
                .claim(CLAIM_TYPE, TYPE_GOOGLE_LINK)
                .claim("googleId", googleId)
                .claim("email", email)
                .claim("fullName", fullName)
                .claim("avatar", avatarUrl)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + GOOGLE_LINK_TTL_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseGoogleLinkToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        if (!TYPE_GOOGLE_LINK.equals(claims.get(CLAIM_TYPE))) {
            throw new JwtException("Token không phải Google link verification");
        }
        return claims;
    }

    // Lấy userId từ token
    public UUID getUserIdFromToken(String token) {
        String subject = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        return UUID.fromString(subject);
    }

    // Kiểm tra token có hợp lệ không
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
