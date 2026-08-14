package com.asterion.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public final class RefreshToken {

    private final RefreshTokenId id;
    private final UserId userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private final Instant createdAt;
    private final Instant revokedAt;
    private final UUID replacedByTokenId;

    public RefreshToken(
            RefreshTokenId id,
            UserId userId,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt,
            Instant revokedAt,
            UUID replacedByTokenId
    ) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.revokedAt = revokedAt;
        this.replacedByTokenId = replacedByTokenId;
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
                null,
                null
        );
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public RefreshToken revoke(UUID replacementTokenId) {
        return new RefreshToken(
                id,
                userId,
                tokenHash,
                expiresAt,
                createdAt,
                Instant.now(),
                replacementTokenId
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

    public UUID replacedByTokenId() {
        return replacedByTokenId;
    }
}