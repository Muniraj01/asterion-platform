package com.asterion.auth.api.request;

public record LoginRequest(
        String email,
        String password
) {
}