package com.asterion.auth.api.response;

public record CurrentUserResponse(
        String email,
        boolean authenticated
) {
}