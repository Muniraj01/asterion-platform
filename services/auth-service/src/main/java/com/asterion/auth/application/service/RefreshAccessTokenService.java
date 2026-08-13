package com.asterion.auth.application.service;

import com.asterion.auth.application.port.in.RefreshAccessTokenCommand;
import com.asterion.auth.application.port.in.RefreshAccessTokenUseCase;
import com.asterion.auth.application.port.out.JwtTokenProvider;
import com.asterion.auth.application.port.out.RefreshTokenRepository;
import com.asterion.auth.application.port.out.TokenHasher;
import com.asterion.auth.application.port.out.UserRepository;
import com.asterion.auth.domain.exception.InvalidCredentialsException;
import com.asterion.auth.domain.model.RefreshToken;
import com.asterion.auth.domain.model.User;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshAccessTokenService
        implements RefreshAccessTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenHasher tokenHasher;

    public RefreshAccessTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            JwtTokenProvider jwtTokenProvider,
            TokenHasher tokenHasher
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenHasher = tokenHasher;
    }

    @Override
    public String refresh(
            RefreshAccessTokenCommand command
    ) {

        String tokenHash = tokenHasher.hash(
                command.refreshToken()
        );

        RefreshToken refreshToken =
                refreshTokenRepository.findByTokenHash(tokenHash)
                        .orElseThrow(
                                InvalidCredentialsException::new
                        );

        if (refreshToken.isExpired()
                || refreshToken.isRevoked()) {

            throw new InvalidCredentialsException();
        }

        User user = userRepository.findById(
                        refreshToken.userId()
                )
                .orElseThrow(
                        InvalidCredentialsException::new
                );

        return jwtTokenProvider.generateAccessToken(user);
    }

    @Override
    public String issue(User user) {

        String rawRefreshToken = UUID.randomUUID().toString();
        String tokenHash = tokenHasher.hash(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.issue(
                user.id(),
                tokenHash,
                Instant.now().plus(Duration.ofDays(30))
        );

        refreshTokenRepository.save(refreshToken);

        return rawRefreshToken;
    }
}