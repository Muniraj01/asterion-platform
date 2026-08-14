package com.asterion.auth.application.service;

import com.asterion.auth.application.port.in.LogoutUseCase;
import com.asterion.auth.application.port.out.RefreshTokenRepository;
import com.asterion.auth.application.port.out.TokenHasher;
import org.springframework.stereotype.Service;

@Service
public class LogoutService implements LogoutUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasher tokenHasher;

    public LogoutService(RefreshTokenRepository refreshTokenRepository,
                         TokenHasher tokenHasher) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHasher = tokenHasher;
    }

    @Override
    public void logout(String rawRefreshToken) {
        String hash = tokenHasher.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash)
                .ifPresent(token ->
                        refreshTokenRepository.revoke(token.revoke(null)));
    }
}