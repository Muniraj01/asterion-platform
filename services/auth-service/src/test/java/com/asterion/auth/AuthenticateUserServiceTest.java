package com.asterion.auth;

import com.asterion.auth.application.port.in.AuthenticateUserCommand;
import com.asterion.auth.application.port.out.PasswordHasher;
import com.asterion.auth.application.port.out.UserRepository;
import com.asterion.auth.application.service.AuthenticateUserService;
import com.asterion.auth.domain.exception.InvalidCredentialsException;
import com.asterion.auth.domain.model.EmailAddress;
import com.asterion.auth.domain.model.PasswordHash;
import com.asterion.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    private AuthenticateUserService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticateUserService(userRepository, passwordHasher);
    }

    @Test
    void shouldAuthenticateActiveUserWithValidCredentials() {
        User user = activeUser();

        when(userRepository.findByEmail(any(EmailAddress.class)))
                .thenReturn(Optional.of(user));
        when(passwordHasher.matches("correct-password", user.passwordHash()))
                .thenReturn(true);

        AuthenticateUserCommand command =
                new AuthenticateUserCommand(
                        user.email().value(),
                        "correct-password"
                );

        User result = service.authenticate(command);

        assertSame(user, result);

        verify(userRepository).findByEmail(any(EmailAddress.class));
        verify(passwordHasher).matches("correct-password", user.passwordHash());
    }

    @Test
    void shouldRejectUnknownUser() {
        when(userRepository.findByEmail(any(EmailAddress.class)))
                .thenReturn(Optional.empty());

        AuthenticateUserCommand command =
                new AuthenticateUserCommand(
                        "unknown@example.com",
                        "password"
                );

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.authenticate(command)
        );

        verify(userRepository).findByEmail(any(EmailAddress.class));
        verifyNoInteractions(passwordHasher);
    }

    @Test
    void shouldRejectInvalidPassword() {
        User user = activeUser();

        when(userRepository.findByEmail(any(EmailAddress.class)))
                .thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong-password", user.passwordHash()))
                .thenReturn(false);

        AuthenticateUserCommand command =
                new AuthenticateUserCommand(
                        user.email().value(),
                        "wrong-password"
                );

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.authenticate(command)
        );

        verify(passwordHasher).matches(
                "wrong-password",
                user.passwordHash()
        );
    }

    @Test
    void shouldRejectInactiveUser() {
        User user = activeUser();
        user.deactivate();

        when(userRepository.findByEmail(any(EmailAddress.class)))
                .thenReturn(Optional.of(user));

        AuthenticateUserCommand command =
                new AuthenticateUserCommand(
                        user.email().value(),
                        "correct-password"
                );

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.authenticate(command)
        );

        verify(userRepository).findByEmail(any(EmailAddress.class));
        verifyNoInteractions(passwordHasher);
    }

    private User activeUser() {
        return User.register(
                new EmailAddress("test@example.com"),
                new PasswordHash("encoded-password")
        );
    }
}
