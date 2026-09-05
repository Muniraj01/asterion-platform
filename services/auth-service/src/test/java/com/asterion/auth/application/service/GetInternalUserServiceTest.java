package com.asterion.auth.application.service;

import com.asterion.auth.application.port.in.InternalUserResult;
import com.asterion.auth.application.port.out.UserRepository;
import com.asterion.auth.domain.model.EmailAddress;
import com.asterion.auth.domain.model.PasswordHash;
import com.asterion.auth.domain.model.Role;
import com.asterion.auth.domain.model.User;
import com.asterion.auth.domain.model.UserId;
import com.asterion.auth.domain.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class GetInternalUserServiceTest {

    private UserRepository userRepository;

    private GetInternalUserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new GetInternalUserService(userRepository);
    }

    @Test
    void shouldReturnInternalUserWhenUserExists() {
        // given
        UserId userId = UserId.newId();
        Instant createdAt = Instant.now();

        User user = new User(
                userId,
                new EmailAddress("user@example.com"),
                new PasswordHash("hashed-password"),
                createdAt,
                true,
                EnumSet.of(Role.USER)
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        // when
        InternalUserResult result = service.getUser(userId);

        // then
        assertEquals(userId, result.userId());
        assertEquals("user@example.com", result.email());
        assertTrue(result.active());
        assertEquals(createdAt, result.createdAt());
        assertEquals(Set.of(Role.USER), result.roles());

        verify(userRepository).findById(userId);
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        // given
        UserId userId = UserId.newId();
        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());
        // when / then
        assertThrows(UserNotFoundException.class,
                () -> service.getUser(userId));
        verify(userRepository).findById(userId);
    }
}