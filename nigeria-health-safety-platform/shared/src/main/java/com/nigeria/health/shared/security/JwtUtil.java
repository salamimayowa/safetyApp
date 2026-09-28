package com.nigeria.health.shared.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT token creation and validation utility.
 *
 * ACCESS TOKEN  — short lived (24 hours), used for API authentication.
 * REFRESH TOKEN — long lived (7 days), used to get new access tokens.
 *
 * Both tokens embed:
 *   - sub: user's email
 *   - userId: user's UUID
 *   - role: user's role (CITIZEN, DONOR, HOSPITAL_ADMIN, etc.)
 *   - type: ACCESS or REFRESH (prevents using refresh token as access token)
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs; // 86400000 = 24 hours

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs; // 604800000 = 7 days

    // ─── Token Generation ───────────────────────────────────────────

    /**
     * Generate an access token for a successfully authenticated user.
     */
    public String generateAccessToken(String email, UUID userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("role", role);
        claims.put("type", "ACCESS");

        return buildToken(claims, email, accessTokenExpiryMs);
    }

    /**
     * Generate a refresh token. Stored (hashed) in DB so it can be invalidated.
     */
    public String generateRefreshToken(String email, UUID userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("type", "REFRESH");

        return buildToken(claims, email, refreshTokenExpiryMs);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expiryMs) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(getSigningKey())
                .compact();
    }

    // ─── Token Validation ───────────────────────────────────────────

    /**
     * Returns true if the token is valid (signature correct + not expired).
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT error: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Check token is specifically an ACCESS token (not a refresh token used as access).
     */
    public boolean isAccessToken(String token) {
        try {
            String type = (String) parseClaims(token).get("type");
            return "ACCESS".equals(type);
        } catch (JwtException e) {
            return false;
        }
    }

    // ─── Claims Extraction ──────────────────────────────────────────

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public UUID extractUserId(String token) {
        String userId = (String) parseClaims(token).get("userId");
        return UUID.fromString(userId);
    }

    public String extractRole(String token) {
        return (String) parseClaims(token).get("role");
    }

    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    // ─── Internal Helpers ───────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
