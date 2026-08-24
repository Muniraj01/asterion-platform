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
import org.springframework.http.HttpHeaders;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DownstreamServerErrorGatewayTest {

    private static MockWebServer mockAuthService;

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
    void shouldPropagateDownstreamServerError() throws InterruptedException {
        mockAuthService.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                {
                  "error": "auth-service failure"
                }
                """));

        mockAuthService.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                {
                  "error": "auth-service failure"
                }
                """));

        mockAuthService.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                {
                  "error": "auth-service failure"
                }
                """));

        String token = createValidToken();

        webTestClient
                .get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus()
                .isEqualTo(500)
                .expectHeader()
                .contentType("application/json")
                .expectBody()
                .jsonPath("$.error")
                .isEqualTo("auth-service failure");

        RecordedRequest firstRequest = mockAuthService.takeRequest(5, TimeUnit.SECONDS);
        RecordedRequest secondRequest = mockAuthService.takeRequest(5, TimeUnit.SECONDS);
        RecordedRequest thirdRequest = mockAuthService.takeRequest(5, TimeUnit.SECONDS);

        assertThat(firstRequest).isNotNull();
        assertThat(secondRequest).isNotNull();
        assertThat(thirdRequest).isNotNull();
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
