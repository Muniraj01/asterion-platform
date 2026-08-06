package com.asterion.auth.api.response;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}