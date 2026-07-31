package com.asterion.auth.domain.exception;

public class InvalidPasswordHashException extends RuntimeException {

    public InvalidPasswordHashException(String value) {
        super("Invalid password hash: " + value);
    }
}