package com.asterion.auth.interfaces.rest;

import java.time.Instant;

public record UserResponse(
        String id,
        String email,
        Instant createdAt,
        boolean active
) {
}