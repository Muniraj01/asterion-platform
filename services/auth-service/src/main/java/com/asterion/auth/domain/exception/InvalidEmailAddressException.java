package com.asterion.auth.domain.exception;

public class InvalidEmailAddressException extends RuntimeException {

    public InvalidEmailAddressException(String value) {
        super("Invalid email address: " + value);
    }
}