package com.asterion.auth.application.port.in;

public record RegisterUserCommand(
        String email,
        String rawPassword
) {
}