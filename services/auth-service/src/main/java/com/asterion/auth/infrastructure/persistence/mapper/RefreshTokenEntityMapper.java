package com.asterion.auth.infrastructure.persistence.mapper;

import com.asterion.auth.domain.model.RefreshToken;
import com.asterion.auth.domain.model.RefreshTokenId;
import com.asterion.auth.domain.model.UserId;
import com.asterion.auth.infrastructure.persistence.entity.RefreshTokenEntity;

public final class RefreshTokenEntityMapper {

    private RefreshTokenEntityMapper() {
    }

    public static RefreshTokenEntity toEntity(RefreshToken domain) {

        RefreshTokenEntity entity = new RefreshTokenEntity();

        entity.setId(domain.id().value());
        entity.setUserId(domain.userId().value());
        entity.setTokenHash(domain.tokenHash());
        entity.setExpiresAt(domain.expiresAt());
        entity.setCreatedAt(domain.createdAt());
        entity.setRevokedAt(domain.revokedAt());

        return entity;
    }

    public static RefreshToken toDomain(RefreshTokenEntity entity) {

        return new RefreshToken(
                new RefreshTokenId(entity.getId()),
                new UserId(entity.getUserId()),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getRevokedAt()
        );
    }
}