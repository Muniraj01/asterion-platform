package com.asterion.auth.api.controller;

import com.asterion.auth.api.error.GlobalExceptionHandler;
import com.asterion.auth.application.port.in.GetInternalUserUseCase;
import com.asterion.auth.application.port.in.InternalUserResult;
import com.asterion.auth.domain.exception.UserNotFoundException;
import com.asterion.auth.domain.model.Role;
import com.asterion.auth.domain.model.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InternalUserControllerTest {

    private final GetInternalUserUseCase useCase = mock(GetInternalUserUseCase.class);
    private final InternalUserController controller = new InternalUserController(useCase);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void shouldReturnUserWhenUserExists() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        InternalUserResult result = new InternalUserResult(
                new UserId(userId),
                "user@example.com",
                true,
                Instant.parse("2026-01-01T00:00:00Z"),
                Set.of(Role.USER)
        );

        when(useCase.getUser(new UserId(userId))).thenReturn(result);
        // when / then
        mockMvc.perform(get("/api/v1/internal/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt")
                        .value("2026-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        when(useCase.getUser(new UserId(userId)))
                .thenThrow(new UserNotFoundException());

        // when / then
        mockMvc.perform(get("/api/v1/internal/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User not found"))
                .andExpect(jsonPath("$.detail").value("User not found"))
                .andExpect(jsonPath("$.type")
                        .value("https://api.asterion.com/problems/user-not-found"))
                .andExpect(jsonPath("$.instance")
                        .value("/api/v1/internal/users/" + userId));
    }
}