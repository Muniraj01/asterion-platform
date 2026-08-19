package com.asterion.auth.application.service;

import com.asterion.auth.application.port.in.RefreshAccessTokenCommand;
import com.asterion.auth.application.port.in.RefreshAccessTokenUseCase;
import com.asterion.auth.application.port.in.TokenPair;
import com.asterion.auth.application.port.out.JwtTokenProvider;
import com.asterion.auth.application.port.out.RefreshTokenRepository;
import com.asterion.auth.application.port.out.TokenHasher;
import com.asterion.auth.application.port.out.UserRepository;
import com.asterion.auth.domain.exception.InvalidCredentialsException;
import com.asterion.auth.domain.model.RefreshToken;
import com.asterion.auth.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    @Override
    public TokenPair refresh(
            RefreshAccessTokenCommand command
    ) {
        String tokenHash = tokenHasher.hash(
                command.refreshToken()
        );

        RefreshToken current =
                refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                        .orElseThrow(InvalidCredentialsException::new);

        if (current.isExpired()) {
            throw new InvalidCredentialsException();
        }

        if (current.isRevoked()) {
            if (current.replacedByTokenId() != null) {
                refreshTokenRepository.revokeFamily(
                        current.familyId()
                );
            }
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findById(
                        current.userId()
                )
                .orElseThrow(
                        InvalidCredentialsException::new
                );

        // Create replacement refresh token
        String newRawRefreshToken =
                UUID.randomUUID().toString();

        RefreshToken replacement =
                RefreshToken.issue(
                        user.id(),
                        tokenHasher.hash(newRawRefreshToken),
                        Instant.now().plus(Duration.ofDays(30)),
                        current.familyId()
                );

        refreshTokenRepository.save(replacement);

        // Revoke old token
        refreshTokenRepository.revoke(
                current.revoke(replacement.id().value())
        );

        // Create new access token
        String newAccessToken =
                jwtTokenProvider.generateAccessToken(user);

        return new TokenPair(
                newAccessToken,
                newRawRefreshToken
        );
    }

    @Override
    public String issue(User user) {

        String rawRefreshToken = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.issue(
                user.id(),
                tokenHasher.hash(rawRefreshToken),
                Instant.now().plus(Duration.ofDays(30))
        );

        refreshTokenRepository.save(refreshToken);

        return rawRefreshToken;
    }
}