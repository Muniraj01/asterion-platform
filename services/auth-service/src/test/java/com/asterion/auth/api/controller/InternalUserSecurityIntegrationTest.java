package com.asterion.auth.api.controller;

import com.asterion.auth.application.port.out.UserRepository;
import com.asterion.auth.domain.model.EmailAddress;
import com.asterion.auth.domain.model.PasswordHash;
import com.asterion.auth.domain.model.Role;
import com.asterion.auth.domain.model.User;
import com.asterion.auth.domain.model.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalUserSecurityIntegrationTest {

    private static final String SERVICE_NAME = "api-gateway";
    private static final String SERVICE_TOKEN = "test-internal-service-token";

    private static final String REPORTING_SERVICE_NAME = "reporting-service";
    private static final String REPORTING_SERVICE_TOKEN = "test-reporting-service-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @Test
    void shouldRejectRequestWhenServiceCredentialsAreMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/internal/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectRequestWhenServiceTokenIsInvalid() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/internal/users/{userId}", userId)
                        .header("X-Service-Name", SERVICE_NAME)
                        .header("X-Service-Token", "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectRequestWhenServiceNameIsInvalid() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/internal/users/{userId}", userId)
                        .header("X-Service-Name", "untrusted-service")
                        .header("X-Service-Token", SERVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowRequestWhenServiceCredentialsAreValid() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        User user = new User(new UserId(userId),
                new EmailAddress("user@example.com"),
                new PasswordHash("hashed-password"),
                createdAt,
                true,
                EnumSet.of(Role.USER));

        when(userRepository.findById(new UserId(userId)))
                .thenReturn(Optional.of(user));

        // when / then
        mockMvc.perform(get("/api/v1/internal/users/{userId}", userId)
                        .header("X-Service-Name", SERVICE_NAME)
                        .header("X-Service-Token", SERVICE_TOKEN)
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
    void shouldRejectRequestWhenTrustedServiceLacksUserReadPermission()
            throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        User user = new User(new UserId(userId),
                new EmailAddress("user@example.com"),
                new PasswordHash("hashed-password"),
                createdAt,
                true,
                EnumSet.of(Role.USER));

        when(userRepository.findById(new UserId(userId)))
                .thenReturn(Optional.of(user));

        // when / then
        mockMvc.perform(get("/api/v1/internal/users/{userId}", userId)
                        .header("X-Service-Name", REPORTING_SERVICE_NAME)
                        .header("X-Service-Token", REPORTING_SERVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnNotFoundWhenServiceCredentialsAreValidButUserDoesNotExist()
            throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(new UserId(userId)))
                .thenReturn(Optional.empty());

        // when / then
        mockMvc.perform(get("/api/v1/internal/users/{userId}", userId)
                        .header("X-Service-Name", SERVICE_NAME)
                        .header("X-Service-Token", SERVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User not found"))
                .andExpect(jsonPath("$.detail").value("User not found"))
                .andExpect(jsonPath("$.type")
                        .value("https://api.asterion.com/problems/user-not-found"))
                .andExpect(jsonPath("$.instance")
                        .value("/api/v1/internal/users/" + userId));
    }

    @Test
    void shouldUseServiceAuthenticationEvenWhenClientJwtIsPresent() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        User user = new User(
                new UserId(userId),
                new EmailAddress("user@example.com"),
                new PasswordHash("hashed-password"),
                createdAt,
                true,
                EnumSet.of(Role.USER)
        );

        when(userRepository.findById(new UserId(userId)))
                .thenReturn(Optional.of(user));
        // when / then
        mockMvc.perform(get("/api/v1/internal/users/{userId}", userId)
                        .header("X-Service-Name", SERVICE_NAME)
                        .header("X-Service-Token", SERVICE_TOKEN)
                        .header("Authorization", "Bearer intentionally-invalid-client-jwt")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }
}