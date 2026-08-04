package com.asterion.auth.application.service;

import com.asterion.auth.application.command.AuthenticateUserCommand;
import com.asterion.auth.application.port.in.AuthenticateUserUseCase;
import com.asterion.auth.application.port.out.PasswordHasher;
import com.asterion.auth.application.port.out.UserRepository;
import com.asterion.auth.domain.exception.InvalidCredentialsException;
import com.asterion.auth.domain.model.EmailAddress;
import com.asterion.auth.domain.model.User;
import org.springframework.stereotype.Service;

@Service
public class AuthenticateUserService
        implements AuthenticateUserUseCase {

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
        EmailAddress email =
                new EmailAddress(command.email());

        User user = userRepository.findByEmail(email)
                .orElseThrow(
                        InvalidCredentialsException::new
                );

        boolean valid = passwordHasher.matches(
                command.password(),
                user.passwordHash()
        );

        if (!valid) {
            throw new InvalidCredentialsException();
        }

        return user;
    }
}