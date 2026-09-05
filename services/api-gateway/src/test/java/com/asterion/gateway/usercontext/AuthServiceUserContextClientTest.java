package com.asterion.gateway.usercontext;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class AuthServiceUserContextClientTest {

    private MockWebServer mockAuthService;
    private AuthServiceUserContextClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockAuthService = new MockWebServer();
        mockAuthService.start();

        client = new AuthServiceUserContextClient(
                WebClient.builder(),
                mockAuthService.url("/").toString(),
                "api-gateway",
                "change-me-internal-service-token"
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockAuthService.shutdown();
    }

    @Test
    void shouldResolveUserContextFromAuthService() throws Exception {
        mockAuthService.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "userId": "11111111-1111-1111-1111-111111111111",
                      "email": "test@example.com",
                      "active": true,
                      "createdAt": "2026-01-01T00:00:00Z",
                      "roles": ["USER"]
                    }
                    """));

        UserContext result = client.getUserContext(
                UUID.fromString("11111111-1111-1111-1111-111111111111")).block();

        assertThat(result).isNotNull();
        assertThat(result.userId())
                .isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(result.email())
                .isEqualTo("test@example.com");
        assertThat(result.active())
                .isTrue();
        assertThat(result.roles())
                .containsExactly("USER");

        RecordedRequest request =
                mockAuthService.takeRequest(5, TimeUnit.SECONDS);

        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath())
                .isEqualTo("/api/v1/internal/users/" +
                        "11111111-1111-1111-1111-111111111111");

        assertThat(request.getHeader("X-Service-Name"))
                .isEqualTo("api-gateway");
        assertThat(request.getHeader("X-Service-Token"))
                .isEqualTo("change-me-internal-service-token");
        assertThat(request.getHeader("Authorization"))
                .isNull();
    }

    @Test
    void shouldFailWhenUserDoesNotExist() {
        mockAuthService.enqueue(new MockResponse().setResponseCode(404));
        assertThatThrownBy(() -> client.getUserContext(
                UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .block())
                .isInstanceOf(WebClientResponseException.NotFound.class);
    }

    @Test
    void shouldFailWhenAuthServiceReturnsServerError() {
        mockAuthService.enqueue(new MockResponse().setResponseCode(500));
        assertThatThrownBy(() -> client.getUserContext(
                UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .block())
                .isInstanceOf(WebClientResponseException.InternalServerError.class);
    }
}