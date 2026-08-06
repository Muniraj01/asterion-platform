package com.asterion.auth.api.error;

import com.asterion.auth.domain.exception.EmailAlreadyRegisteredException;
import com.asterion.auth.domain.exception.InvalidCredentialsException;
import com.asterion.auth.domain.exception.InvalidEmailAddressException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(
            EmailAlreadyRegisteredException.class
    )
    public ProblemDetail handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException ex,
            HttpServletRequest request
    ) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );

        problem.setTitle("Email already registered");

        problem.setType(URI.create(
                "https://api.asterion.com/problems/email-already-registered"
        ));

        problem.setProperty("timestamp", Instant.now());

        problem.setProperty("instance",
                request.getRequestURI());

        return problem;
    }

    @ExceptionHandler(
            InvalidCredentialsException.class
    )
    public ProblemDetail handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request
    ) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage()
        );

        problem.setTitle("Invalid credentials");

        problem.setType(URI.create(
                "https://api.asterion.com/problems/invalid-credentials"
        ));

        problem.setProperty("timestamp", Instant.now());

        problem.setProperty("instance",
                request.getRequestURI());

        return problem;
    }

    @ExceptionHandler(
            InvalidEmailAddressException.class
    )
    public ProblemDetail handleInvalidEmail(
            InvalidEmailAddressException ex,
            HttpServletRequest request
    ) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );

        problem.setTitle("Invalid email address");

        problem.setType(URI.create(
                "https://api.asterion.com/problems/invalid-email-address"
        ));

        problem.setProperty("timestamp", Instant.now());

        problem.setProperty("instance",
                request.getRequestURI());

        return problem;
    }

    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        List<ApiValidationError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed"
        );

        problem.setTitle("Validation failed");

        problem.setType(URI.create(
                "https://api.asterion.com/problems/validation-failed"
        ));

        problem.setProperty("timestamp", Instant.now());

        problem.setProperty("instance",
                request.getRequestURI());

        problem.setProperty("errors", errors);

        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );

        problem.setTitle("Internal server error");

        problem.setType(URI.create(
                "https://api.asterion.com/problems/internal-server-error"
        ));

        problem.setProperty("timestamp", Instant.now());

        problem.setProperty("instance",
                request.getRequestURI());

        return problem;
    }

    private ApiValidationError mapFieldError(
            FieldError error
    ) {

        return new ApiValidationError(
                error.getField(),
                error.getDefaultMessage()
        );
    }
}