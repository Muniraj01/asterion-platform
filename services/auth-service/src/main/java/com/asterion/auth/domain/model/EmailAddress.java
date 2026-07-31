package com.asterion.auth.domain.model;

import com.asterion.auth.domain.exception.InvalidEmailAddressException;

import java.util.Objects;
import java.util.regex.Pattern;

public record EmailAddress(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public EmailAddress {
        Objects.requireNonNull(value, "email value must not be null");

        String normalized = value.trim().toLowerCase();

        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new InvalidEmailAddressException(value);
        }

        value = normalized;
    }

    @Override
    public String toString() {
        return value;
    }
}