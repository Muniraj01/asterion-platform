package com.asterion.auth.api.response;

import java.time.Instant;

public record UserResponse(
        String id,
        String email,
        Instant createdAt,
        boolean active
) {
}