package com.asterion.auth.domain.model;

import java.time.Instant;
import java.util.Objects;

public final class RefreshToken {

    private final RefreshTokenId id;
    private final UserId userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private final Instant createdAt;
    private final Instant revokedAt;

    public RefreshToken(
            RefreshTokenId id,
            UserId userId,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt,
            Instant revokedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.revokedAt = revokedAt;
    }

    public static RefreshToken issue(
            UserId userId,
            String tokenHash,
            Instant expiresAt
    ) {
        return new RefreshToken(
                RefreshTokenId.newId(),
                userId,
                tokenHash,
                expiresAt,
                Instant.now(),
                null
        );
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public RefreshToken revoke() {
        return new RefreshToken(
                id,
                userId,
                tokenHash,
                expiresAt,
                createdAt,
                Instant.now()
        );
    }

    public RefreshTokenId id() {
        return id;
    }

    public UserId userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }
}