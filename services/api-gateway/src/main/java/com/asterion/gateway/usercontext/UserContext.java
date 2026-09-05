package com.asterion.gateway.usercontext;

import java.util.Set;
import java.util.UUID;

public record UserContext(
        UUID userId,
        String email,
        boolean active,
        Set<String> roles
) {
}