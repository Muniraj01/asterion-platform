package com.asterion.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthServiceCircuitBreakerGatewayTest {

    private static MockWebServer mockAuthService;
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void startMockAuthService() throws IOException {
        mockAuthService = new MockWebServer();
        mockAuthService.start();
    }

    @AfterAll
    static void stopMockAuthService() throws IOException {
        mockAuthService.shutdown();
    }

    @BeforeEach
    void clearPendingRequests() throws InterruptedException {
        while (mockAuthService.takeRequest(10, TimeUnit.MILLISECONDS) != null) {
            // Drain requests left by the previous test.
        }
        circuitBreakerRegistry
                .circuitBreaker("authServiceCircuitBreaker")
                .reset();
    }

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry registry) {
        registry.add("ASTERION_AUTH_SERVICE_URL",
                () -> mockAuthService.url("/").toString());
    }

    @Test
    void shouldOpenCircuitAfterRepeatedDownstreamFailures() throws InterruptedException {
        // Five downstream failures are enough to fill the count-based
        // circuit-breaker window and trip the circuit.
        for (int i = 0; i < 5; i++) {
            mockAuthService.enqueue(new MockResponse()
                    .setResponseCode(500)
                    .setBody("""
                    {
                      "error": "downstream failure"
                    }
                    """));
        }

        String token = createValidToken();

        // Generate five failed Gateway requests.
        for (int i = 0; i < 5; i++) {
            webTestClient
                    .get()
                    .uri("/api/v1/users/me")
                    .header("Authorization", "Bearer " + token)
                    .exchange()
                    .expectStatus()
                    .is5xxServerError();
        }

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker("authServiceCircuitBreaker");

        // The important assertion: the five failures opened the circuit.
        assertThat(circuitBreaker.getState())
                .as("Circuit breaker should be OPEN after five downstream failures")
                .isEqualTo(CircuitBreaker.State.OPEN);

        // The next request must be rejected by the OPEN circuit.
        webTestClient
                .get()
                .uri("/api/v1/users/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isEqualTo(503);

        // Drain whatever requests belong to the five expected failures.
        // We deliberately do not assert an exact number here because
        // Gateway/Netty processing is asynchronous.
        int downstreamRequests = 0;
        RecordedRequest request;

        while ((request = mockAuthService.takeRequest(100, TimeUnit.MILLISECONDS)) != null) {
            downstreamRequests++;

            assertThat(request.getMethod()).isEqualTo("GET");
            assertThat(request.getPath()).isEqualTo("/api/v1/users/me");
        }

        assertThat(downstreamRequests)
                .as("The circuit must have opened after the five failed calls")
                .isGreaterThanOrEqualTo(5);

        // Give the gateway a short period to prove that the rejected request
        // did not create another downstream call.
        assertThat(mockAuthService.takeRequest(500, TimeUnit.MILLISECONDS))
                .as("An OPEN circuit must prevent the rejected request from reaching Auth Service")
                .isNull();
    }

    private String createValidToken() {
        SecretKey key = Keys.hmacShaKeyFor(
                "change-me-change-me-change-me-change-me".getBytes(StandardCharsets.UTF_8));

        Instant now = Instant.now();
        return Jwts.builder()
                .subject("11111111-1111-1111-1111-111111111111")
                .claim("email", "test@example.com")
                .claim("roles", List.of("USER"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(key)
                .compact();
    }
}