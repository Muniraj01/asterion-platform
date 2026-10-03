package com.asterion.order.infrastructure.security;

import java.util.UUID;

public record OrderUserIdentity(
        UUID userId,
        String email,
        String roles
) {
}