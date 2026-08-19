package com.asterion.auth.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class RefreshToken {

    private final RefreshTokenId id;
    private final UserId userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private final Instant createdAt;
    private final Instant revokedAt;
    private final UUID replacedByTokenId;
    private final UUID familyId;

    public RefreshToken(
            RefreshTokenId id,
            UserId userId,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt,
            Instant revokedAt,
            UUID replacedByTokenId,
            UUID familyId
    ) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.revokedAt = revokedAt;
        this.replacedByTokenId = replacedByTokenId;
        this.familyId = Objects.requireNonNull(familyId);
    }

    public static RefreshToken issue(
            UserId userId,
            String tokenHash,
            Instant expiresAt
    ) {
        return issue(
                userId,
                tokenHash,
                expiresAt,
                UUID.randomUUID()
        );
    }

    public static RefreshToken issue(
            UserId userId,
            String tokenHash,
            Instant expiresAt,
            UUID familyId
    ) {
        return new RefreshToken(
                RefreshTokenId.newId(),
                userId,
                tokenHash,
                expiresAt,
                Instant.now(),
                null,
                null,
                familyId
        );
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public RefreshToken revoke(UUID replacementTokenId) {
        if (isRevoked()) {
            return this;
        }

        return new RefreshToken(
                id,
                userId,
                tokenHash,
                expiresAt,
                createdAt,
                Instant.now(),
                replacementTokenId,
                familyId
        );
    }

    public RefreshToken revoke() {
        if (isRevoked()) {
            return this;
        }

        return new RefreshToken(
                id,
                userId,
                tokenHash,
                expiresAt,
                createdAt,
                Instant.now(),
                replacedByTokenId,
                familyId
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

    public UUID familyId() {
        return familyId;
    }
}