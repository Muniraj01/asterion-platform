package com.asterion.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DownstreamRetryGatewayTest {

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

    @Test
    void shouldRetryTransientDownstreamFailureAndEventuallySucceed() throws InterruptedException {
        mockAuthService.enqueue(new MockResponse()
                .setResponseCode(503)
                .setBody("""
                        {
                          "error": "service temporarily unavailable"
                        }
                        """));

        mockAuthService.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "email": "test@example.com"
                        }
                        """));

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

        RecordedRequest firstRequest = mockAuthService.takeRequest(5, TimeUnit.SECONDS);
        RecordedRequest secondRequest = mockAuthService.takeRequest(5, TimeUnit.SECONDS);

        assertThat(firstRequest).isNotNull();
        assertThat(secondRequest).isNotNull();

        assertThat(firstRequest.getPath()).isEqualTo("/api/v1/users/me");
        assertThat(secondRequest.getPath()).isEqualTo("/api/v1/users/me");
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