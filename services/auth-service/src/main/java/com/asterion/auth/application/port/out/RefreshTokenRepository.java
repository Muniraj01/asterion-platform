package com.asterion.auth.application.port.out;

import com.asterion.auth.domain.model.RefreshToken;
import com.asterion.auth.domain.model.RefreshTokenId;

import java.util.Optional;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findById(RefreshTokenId id);

    Optional<RefreshToken> findByTokenHash(String tokenHash);
}