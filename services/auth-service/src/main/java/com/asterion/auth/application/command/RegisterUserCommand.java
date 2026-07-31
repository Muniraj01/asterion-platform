package com.asterion.auth.application.command;

public record RegisterUserCommand(
        String email,
        String rawPassword
) {
}