package com.asterion.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
class JwtAuthenticationGatewayTest {

    private static MockWebServer mockAuthService;

    @LocalServerPort
    private int port;

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
    void shouldRejectProtectedRequestWithoutJwt() {
        webTestClient
                .get()
                .uri("/api/v1/users/me")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectProtectedRequestWithInvalidJwt() {
        webTestClient
                .get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldAuthenticateRequestWithValidJwt() throws InterruptedException {
        mockAuthService.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "id": "11111111-1111-1111-1111-111111111111",
                      "email": "test@example.com"
                    }
                    """)
        );

        String token = createValidToken(List.of("USER"));
        webTestClient
                .get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();

        var recordedRequest = mockAuthService.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/v1/users/me");
    }

    private String createValidToken(List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(
                "change-me-change-me-change-me-change-me".getBytes(StandardCharsets.UTF_8));

        Instant now = Instant.now();
        return Jwts.builder()
                .subject("11111111-1111-1111-1111-111111111111")
                .claim("email", "test@example.com")
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(key)
                .compact();
    }

    @Test
    void shouldForwardAuthorizationHeaderToDownstreamService() throws InterruptedException {
        mockAuthResponse();
        String token = createValidToken(List.of("USER"));
        webTestClient
                .get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();

        var recordedRequest = mockAuthService.takeRequest();
        assertThat(recordedRequest.getHeader(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer " + token);
    }

    private static void mockAuthResponse() {
        mockAuthService.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "email": "test@example.com",
                      "authenticated": true
                    }
                    """)
        );
    }

    @Test
    void shouldPropagateAuthenticatedUserIdentityToDownstreamService() throws InterruptedException {
        mockAuthResponse();
        String token = createValidToken(List.of("USER"));
        webTestClient
                .get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();

        var recordedRequest = mockAuthService.takeRequest();
        assertThat(recordedRequest.getHeader("X-User-Id"))
                .isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(recordedRequest.getHeader("X-User-Email"))
                .isEqualTo("test@example.com");
        assertThat(recordedRequest.getHeader("X-User-Roles"))
                .isEqualTo("USER");
    }

    @Test
    void shouldNotTrustClientSuppliedIdentityHeaders() throws InterruptedException {
        mockAuthResponse();
        String token = createValidToken(List.of("USER"));
        webTestClient
                .get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-User-Id", "attacker-user-id")
                .header("X-User-Email", "attacker@example.com")
                .header("X-User-Roles", "ADMIN")
                .exchange()
                .expectStatus()
                .isOk();

        var recordedRequest = mockAuthService.takeRequest();
        assertThat(recordedRequest.getHeader("X-User-Id"))
                .isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(recordedRequest.getHeader("X-User-Email"))
                .isEqualTo("test@example.com");
        assertThat(recordedRequest.getHeader("X-User-Roles"))
                .isEqualTo("USER");
    }

    @Test
    void shouldAllowUserToAccessUserEndpoint() throws InterruptedException {
        mockAuthService.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                {
                  "id": "11111111-1111-1111-1111-111111111111",
                  "email": "test@example.com"
                }
                """)
        );

        String token = createValidToken(List.of("USER"));
        webTestClient
                .get()
                .uri("/api/v1/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();

        mockAuthService.takeRequest();
    }

    @Test
    void shouldRejectUserFromAdminEndpoint() {
        String token = createValidToken(List.of("USER"));
        webTestClient
                .get()
                .uri("/api/v1/admin/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void shouldAllowAdminToAccessAdminEndpoint() throws InterruptedException {
        mockAuthService.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                {
                  "users": []
                }
                """)
        );

        String token = createValidToken(List.of("ADMIN"));
        webTestClient
                .get()
                .uri("/api/v1/admin/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();

        var recordedRequest = mockAuthService.takeRequest();
        assertThat(recordedRequest.getPath()).isEqualTo("/api/v1/admin/users");
    }

    @Test
    void shouldRejectUnauthenticatedAdminRequest() {
        webTestClient
                .get()
                .uri("/api/v1/admin/users")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }
}