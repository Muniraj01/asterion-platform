package com.asterion.auth.application.command;

public record AuthenticateUserCommand(
        String email,
        String password
) {
}