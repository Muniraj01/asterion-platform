package com.asterion.auth.application.port.in;

public record TokenPair(
        String accessToken,
        String refreshToken
) {
}