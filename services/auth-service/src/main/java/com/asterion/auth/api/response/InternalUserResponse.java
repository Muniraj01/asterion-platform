package com.asterion.auth.api.response;

import com.asterion.auth.domain.model.Role;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record InternalUserResponse(
        UUID userId,
        String email,
        boolean active,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant createdAt,

        Set<Role> roles
) {
}