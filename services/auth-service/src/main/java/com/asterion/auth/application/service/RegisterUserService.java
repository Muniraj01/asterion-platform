package com.asterion.auth.application.service;

import com.asterion.auth.application.command.RegisterUserCommand;
import com.asterion.auth.domain.exception.EmailAlreadyRegisteredException;
import com.asterion.auth.application.port.in.RegisterUserUseCase;
import com.asterion.auth.application.port.out.PasswordHasher;
import com.asterion.auth.application.port.out.UserRepository;
import com.asterion.auth.domain.model.EmailAddress;
import com.asterion.auth.domain.model.PasswordHash;
import com.asterion.auth.domain.model.User;
import org.springframework.stereotype.Service;

@Service
public class RegisterUserService implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public RegisterUserService(
            UserRepository userRepository,
            PasswordHasher passwordHasher
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public User register(RegisterUserCommand command) {

        EmailAddress email = new EmailAddress(command.email());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email.value());
        }

        PasswordHash hash =
                new PasswordHash(passwordHasher.hash(command.rawPassword()).value());

        User user = User.register(email, hash);

        return userRepository.save(user);
    }
}