package com.asterion.auth.infrastructure.security;

import com.asterion.auth.domain.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationSeconds;

    public JwtTokenProvider(
            @Value("${security.jwt.secret}")
            String secret,
            @Value("${security.jwt.expiration-seconds:3600}")
            long expirationSeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(User user) {

        Instant now = Instant.now();

        Instant expiry = now.plusSeconds(
                expirationSeconds
        );

        return Jwts.builder()
                .subject(user.id().value().toString())
                .claim("email", user.email().value())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public long expirationSeconds() {
        return expirationSeconds;
    }
}