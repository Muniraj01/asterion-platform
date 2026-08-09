package com.asterion.auth.api.error;

public record ApiValidationError(
        String field,
        String message
) {
}