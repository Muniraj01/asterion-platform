package com.asterion.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RateLimitingGatewayTest {

    private static MockWebServer mockAuthService;
    @Autowired
    private StringRedisTemplate redisTemplate;

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
    void clearTestState() throws InterruptedException {
        while (mockAuthService.takeRequest(10, TimeUnit.MILLISECONDS) != null) {
            // Drain requests left by previous tests.
        }
        // Ensure clean Redis state per test without touching the normal development Redis data.
        redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushDb();
    }

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry registry) {
        registry.add("ASTERION_AUTH_SERVICE_URL", () -> mockAuthService.url("/").toString());
        //use the Redis DB 15 instead of your normal DB for tests (database configured in application.yml)
        registry.add("REDIS_DATABASE", () -> "15");
    }

    @Test
    void shouldRejectRequestWhenRateLimitIsExceeded() throws InterruptedException {
        // The configured bucket will allow 10 requests.
        // Therefore the 11th request should be rejected.
        for (int i = 0; i < 10; i++) {
            mockAuthService.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
                    {
                      "email": "test@example.com"
                    }
                    """));
        }

        String token = createValidToken("11111111-1111-1111-1111-111111111111");
        for (int i = 0; i < 10; i++) {
            webTestClient
                    .get()
                    .uri("/api/v1/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }

        // 11th request must be rejected by the Gateway.
        webTestClient
                .get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus()
                .isEqualTo(429);

        // Exactly ten requests should have reached Auth Service.
        for (int i = 0; i < 10; i++) {
            RecordedRequest request = mockAuthService.takeRequest(2, TimeUnit.SECONDS);
            assertThat(request)
                    .as("Expected downstream request %s", i + 1)
                    .isNotNull();
            assertThat(request.getMethod()).isEqualTo("GET");
            assertThat(request.getPath()).isEqualTo("/api/v1/users/me");
        }

        // The rejected 11th request must not reach Auth Service.
        assertThat(mockAuthService
                .takeRequest(500, TimeUnit.MILLISECONDS))
                .as("Rate-limited request must not reach Auth Service")
                .isNull();
    }

    @Test
    void shouldMaintainIndependentRateLimitsPerUser() {
        for (int i = 0; i < 11; i++) {
            mockAuthService.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
                    {
                      "email": "test@example.com"
                    }
                    """));
        }

        String userOneToken = createValidToken("11111111-1111-1111-1111-111111111111");
        String userTwoToken = createValidToken("22222222-2222-2222-2222-222222222222");

        // Consume user one's entire burst.
        for (int i = 0; i < 10; i++) {
            webTestClient
                    .get()
                    .uri("/api/v1/users/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userOneToken)
                    .exchange()
                    .expectStatus()
                    .isOk();
        }

        // User one is exhausted.
        webTestClient
                .get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userOneToken)
                .exchange()
                .expectStatus()
                .isEqualTo(429);

        // User two has a completely independent bucket.
        webTestClient
                .get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userTwoToken)
                .exchange()
                .expectStatus()
                .isOk();
    }

    private String createValidToken(String subject) {
        SecretKey key = Keys.hmacShaKeyFor(
                "change-me-change-me-change-me-change-me".getBytes(StandardCharsets.UTF_8));

        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim("email", subject + "@example.com")
                .claim("roles", List.of("USER"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(key)
                .compact();
    }
}