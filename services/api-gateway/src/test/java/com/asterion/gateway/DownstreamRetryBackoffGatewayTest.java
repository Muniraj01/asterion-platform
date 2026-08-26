package com.asterion.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import okhttp3.mockwebserver.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DownstreamRetryBackoffGatewayTest  {

    private static MockWebServer mockAuthService;

    @LocalServerPort
    private int gatewayPort;

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

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry registry) {
        registry.add("ASTERION_AUTH_SERVICE_URL",
                () -> mockAuthService.url("/").toString());
    }

    @BeforeEach
    void resetMockAuthService() {
        mockAuthService.setDispatcher(new QueueDispatcher());
    }

    @Test
    void shouldApplyBackoffBetweenRetries() throws InterruptedException {
        List<Long> requestTimes = new CopyOnWriteArrayList<>();

        mockAuthService.setDispatcher(new Dispatcher() {

            private int attempt;

            @Override
            public MockResponse dispatch(RecordedRequest request) {
                requestTimes.add(System.nanoTime());
                attempt++;
                if (attempt <= 2) {
                    return new MockResponse()
                            .setResponseCode(503)
                            .setHeader("Content-Type", "application/json")
                            .setBody("""
                                {
                                  "error": "service temporarily unavailable"
                                }
                                """);
                }

                return new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""
                            {
                              "email": "test@example.com"
                            }
                            """);
            }
        });

        String token = createValidToken();
        webTestClient
                .mutate()
                .responseTimeout(Duration.ofSeconds(10))
                .build()
                .get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.email")
                .isEqualTo("test@example.com");

        assertThat(requestTimes)
                .as("Gateway should make exactly three attempts")
                .hasSize(3);

        long firstBackoffMillis =
                TimeUnit.NANOSECONDS.toMillis(requestTimes.get(1) - requestTimes.get(0));
        long secondBackoffMillis =
                TimeUnit.NANOSECONDS.toMillis(requestTimes.get(2) - requestTimes.get(1));

        assertThat(firstBackoffMillis)
                .as("First retry should be delayed by backoff")
                .isGreaterThanOrEqualTo(40);
        assertThat(secondBackoffMillis)
                .as("Second retry should be delayed by backoff")
                .isGreaterThanOrEqualTo(80);

        assertThat(firstBackoffMillis)
                .as("First retry backoff should not exceed configured maximum")
                .isLessThan(500);
        assertThat(secondBackoffMillis)
                .as("Second retry backoff should not exceed configured maximum")
                .isLessThan(500);
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