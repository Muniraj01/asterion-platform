package com.asterion.auth.domain.model;

import com.asterion.auth.domain.exception.InvalidPasswordHashException;

import java.util.Objects;

public record PasswordHash(String value) {

    public PasswordHash {
        Objects.requireNonNull(value, "password hash must not be null");

        if (value.isBlank()) {
            throw new InvalidPasswordHashException(value);
        }
    }

    @Override
    public String toString() {
        return "[PROTECTED]";
    }
}