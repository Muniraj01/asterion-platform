package com.asterion.auth.application.port.in;

public record AuthenticateUserCommand(
        String email,
        String password
) {
}