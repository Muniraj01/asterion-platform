package com.asterion.auth.application.port.out;

import com.asterion.auth.domain.model.User;

public interface JwtTokenProvider {

    String generateAccessToken(User user);

    String generateRefreshToken(User user);

    boolean isValid(String token);

    String extractSubject(String token);

    String extractEmail(String token);
}