package com.asterion.auth.application.port.out;

import com.asterion.auth.domain.model.User;

import java.util.List;

public interface JwtTokenProvider {

    String generateAccessToken(User user);

    String generateRefreshToken();

    boolean isValid(String token);

    String extractUserId(String token);

    String extractEmail(String token);

    List<String> extractRoles(String token);

    long accessTokenExpirationSeconds();

    long refreshTokenExpirationSeconds();
}