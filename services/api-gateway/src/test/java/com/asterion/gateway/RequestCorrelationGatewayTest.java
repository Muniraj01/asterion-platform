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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RequestCorrelationGatewayTest {

    private static final String JWT_SECRET = "change-me-change-me-change-me-change-me";

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";

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
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("ASTERION_AUTH_SERVICE_URL",
                () -> mockAuthService.url("/").toString()
        );
    }

    @Test
    void shouldPropagateExistingRequestId() throws InterruptedException {

        String requestId = "request-123";

        enqueueMockAuthService();

        webTestClient
                .get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + createValidToken())
                .header("X-Request-Id", requestId)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .valueEquals("X-Request-Id", requestId);

        RecordedRequest downstreamRequest = mockAuthService.takeRequest();

        assertThat(downstreamRequest.getHeader("X-Request-Id")).isEqualTo(requestId);
    }

    private static void enqueueMockAuthService() {
        mockAuthService.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"status\":\"ok\"}")
        );
    }

    @Test
    void shouldGenerateRequestIdWhenMissing() throws InterruptedException {

        enqueueMockAuthService();

        var response = webTestClient
                .get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + createValidToken())
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .exists("X-Request-Id")
                .returnResult(Void.class);

        String requestId = response.getResponseHeaders().getFirst("X-Request-Id");

        assertThat(requestId).isNotBlank();

        RecordedRequest downstreamRequest = mockAuthService.takeRequest();

        assertThat(downstreamRequest.getHeader("X-Request-Id")).isEqualTo(requestId);
    }

    private String createValidToken() {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(USER_ID)
                .claim("email", "test@example.com")
                .claim("roles", List.of("USER"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(key)
                .compact();
    }
}