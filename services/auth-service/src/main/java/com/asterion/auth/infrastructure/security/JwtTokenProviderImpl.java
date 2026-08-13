package com.asterion.auth.infrastructure.security;

import com.asterion.auth.application.port.out.JwtTokenProvider;
import com.asterion.auth.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProviderImpl implements JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpirationSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    private final long refreshTokenExpirationSeconds;

    public JwtTokenProviderImpl(
            @Value("${asterion.security.jwt.secret}") String secret,
            @Value("${asterion.security.jwt.access-token-expiration-seconds}") long accessExp,
            @Value("${asterion.security.jwt.refresh-token-expiration-seconds}") long refreshExp
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationSeconds = accessExp;
        this.refreshTokenExpirationSeconds = refreshExp;
    }

    @Override
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationSeconds * 1000);

        return Jwts.builder()
                .subject(user.id().toString())
                .claim("email", user.email().value())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    @Override
    public String generateRefreshToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public boolean isValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public String extractUserId(String token) {
        return claims(token).getSubject();
    }

    @Override
    public String extractEmail(String token) {
        return claims(token).get("email", String.class);
    }

    @Override
    public long accessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    @Override
    public long refreshTokenExpirationSeconds() {
        return refreshTokenExpirationSeconds;
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}