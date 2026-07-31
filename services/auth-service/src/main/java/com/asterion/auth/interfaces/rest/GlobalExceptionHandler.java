package com.asterion.auth.interfaces.rest;

import com.asterion.auth.application.exception.EmailAlreadyExistsException;
import com.asterion.auth.domain.exception.InvalidEmailAddressException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleDuplicateEmail(
            EmailAlreadyExistsException ex
    ) {
        return Map.of(
                "timestamp", Instant.now(),
                "error", "EMAIL_ALREADY_EXISTS",
                "message", ex.getMessage()
        );
    }

    @ExceptionHandler({
            InvalidEmailAddressException.class,
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(Exception ex) {
        return Map.of(
                "timestamp", Instant.now(),
                "error", "VALIDATION_ERROR",
                "message", ex.getMessage()
        );
    }
}