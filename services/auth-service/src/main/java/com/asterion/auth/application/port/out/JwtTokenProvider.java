package com.asterion.auth.application.port.out;

import com.asterion.auth.domain.model.User;

public interface JwtTokenProvider {

    String generateAccessToken(User user);

    String generateRefreshToken();

    boolean isValid(String token);

    String extractUserId(String token);

    String extractEmail(String token);

    long accessTokenExpirationSeconds();

    long refreshTokenExpirationSeconds();
}