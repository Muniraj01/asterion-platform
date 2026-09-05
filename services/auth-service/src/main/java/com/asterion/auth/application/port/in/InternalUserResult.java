package com.asterion.auth.application.port.in;

import com.asterion.auth.domain.model.Role;
import com.asterion.auth.domain.model.UserId;

import java.time.Instant;
import java.util.Set;

public record InternalUserResult(
        UserId userId,
        String email,
        boolean active,
        Instant createdAt,
        Set<Role> roles
) {
}