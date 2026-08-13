package com.asterion.auth.application.port.in;

public record RefreshAccessTokenCommand(
        String refreshToken
) {
}