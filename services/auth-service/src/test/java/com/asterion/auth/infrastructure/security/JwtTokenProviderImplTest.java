package com.asterion.auth.infrastructure.security;

import com.asterion.auth.domain.model.EmailAddress;
import com.asterion.auth.domain.model.PasswordHash;
import com.asterion.auth.domain.model.Role;
import com.asterion.auth.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderImplTest {

    private static final String SECRET =
            "asterion-test-secret-key-must-be-at-least-32-bytes-long";

    private static final String UUID_PLACEHOLDER =
            "00000000-0000-0000-0000-000000000001";

    private static final long ACCESS_EXPIRATION_SECONDS = 900;

    private static final long REFRESH_EXPIRATION_SECONDS = 604800;

    private JwtTokenProviderImpl tokenProvider;

    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProviderImpl(
                SECRET,
                ACCESS_EXPIRATION_SECONDS,
                REFRESH_EXPIRATION_SECONDS
        );

        secretKey = Keys.hmacShaKeyFor(
                SECRET.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Test
    void shouldGenerateValidAccessToken() {
        User user = adminUser();

        String token = tokenProvider.generateAccessToken(user);

        assertNotNull(token);
        assertTrue(tokenProvider.isValid(token));
    }

    @Test
    void shouldIncludeUserIdInSubject() {
        User user = adminUser();

        String token = tokenProvider.generateAccessToken(user);

        assertEquals(
                user.id().toString(),
                tokenProvider.extractUserId(token)
        );
    }

    @Test
    void shouldIncludeEmailClaim() {
        User user = adminUser();

        String token = tokenProvider.generateAccessToken(user);

        assertEquals(
                user.email().value(),
                tokenProvider.extractEmail(token)
        );
    }

    @Test
    void shouldIncludeUserRoles() {
        User user = adminUser();

        String token = tokenProvider.generateAccessToken(user);

        assertEquals(
                List.of("ADMIN", "USER"),
                tokenProvider.extractRoles(token)
                        .stream()
                        .sorted()
                        .toList()
        );
    }

    @Test
    void shouldSetIssuedAtAndExpiration() {
        User user = adminUser();

        String token = tokenProvider.generateAccessToken(user);

        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());

        long lifetime =
                claims.getExpiration().getTime()
                        - claims.getIssuedAt().getTime();

        assertEquals(
                ACCESS_EXPIRATION_SECONDS * 1000,
                lifetime
        );
    }

    @Test
    void shouldRejectTamperedToken() {
        User user = adminUser();

        String token = tokenProvider.generateAccessToken(user);

        String tamperedToken =
                token.substring(0, token.length() - 1)
                        + (token.endsWith("a") ? "b" : "a");

        assertFalse(tokenProvider.isValid(tamperedToken));
    }

    @Test
    void shouldRejectMalformedToken() {
        assertFalse(
                tokenProvider.isValid("not-a-valid-jwt")
        );
    }

    @Test
    void shouldRejectExpiredToken() {
        Date now = new Date();
        Date expiration =
                new Date(now.getTime() - 1000);

        String token = Jwts.builder()
                .subject(UUID_PLACEHOLDER)
                .claim("email", "admin@asterion.com")
                .claim("roles", List.of("ADMIN", "USER"))
                .issuedAt(
                        new Date(now.getTime() - 2000)
                )
                .expiration(expiration)
                .signWith(secretKey)
                .compact();

        assertFalse(tokenProvider.isValid(token));
    }

    @Test
    void shouldGenerateUniqueRefreshTokens() {
        String first = tokenProvider.generateRefreshToken();
        String second = tokenProvider.generateRefreshToken();

        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(first, second);
    }

    @Test
    void shouldExposeConfiguredTokenExpirations() {
        assertEquals(
                ACCESS_EXPIRATION_SECONDS,
                tokenProvider.accessTokenExpirationSeconds()
        );

        assertEquals(
                REFRESH_EXPIRATION_SECONDS,
                tokenProvider.refreshTokenExpirationSeconds()
        );
    }

    private User adminUser() {
        User user = User.register(
                new EmailAddress("admin@asterion.com"),
                new PasswordHash("encoded-password")
        );

        user.addRole(Role.ADMIN);

        return user;
    }
}