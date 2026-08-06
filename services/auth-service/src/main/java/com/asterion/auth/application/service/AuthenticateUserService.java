package com.asterion.auth.application.service;

import com.asterion.auth.application.command.AuthenticateUserCommand;
import com.asterion.auth.application.port.in.AuthenticateUserUseCase;
import com.asterion.auth.application.port.out.PasswordHasher;
import com.asterion.auth.application.port.out.UserRepository;
import com.asterion.auth.domain.exception.InvalidCredentialsException;
import com.asterion.auth.domain.model.EmailAddress;
import com.asterion.auth.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuthenticateUserService
        implements AuthenticateUserUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(
                    AuthenticateUserService.class
            );

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AuthenticateUserService(
            UserRepository userRepository,
            PasswordHasher passwordHasher
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public User authenticate(
            AuthenticateUserCommand command
    ) {

        log.info("Authenticating user: {}",
                command.email());

        EmailAddress email =
                new EmailAddress(command.email());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found: {}",
                            command.email());

                    return new InvalidCredentialsException();
                });

        log.info("User found: {}",
                user.email().value());

        boolean valid = passwordHasher.matches(
                command.password(),
                user.passwordHash()
        );

        log.info("Password valid: {}", valid);

        if (!valid) {
            throw new InvalidCredentialsException();
        }

        return user;
    }
}