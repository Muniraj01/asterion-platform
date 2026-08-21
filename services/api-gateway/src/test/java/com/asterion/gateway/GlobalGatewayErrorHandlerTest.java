package com.asterion.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GlobalGatewayErrorHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturnStandardErrorResponseForUnknownRoute() {
        String requestId = "test-request-id-123";
        webTestClient
                .get()
                .uri("/api/v1/does-not-exist")
//                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .header("X-Request-Id", requestId)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNAUTHORIZED)
                .expectHeader()
                .contentType("application/json")
                .expectBody()
                .consumeWith(result -> {
                    String body = new String(result.getResponseBody(), StandardCharsets.UTF_8);
                    assertThat(body).contains("\"status\":401");
                    assertThat(body).contains("\"error\":\"Unauthorized\"");
                    assertThat(body).contains("\"message\":\"Authentication is required\"");
                    assertThat(body).contains("\"path\":\"/api/v1/does-not-exist\"");
                    assertThat(body).contains("\"requestId\":\"" + requestId + "\"");
                });
    }

    @Test
    void shouldReturnStandardErrorResponseForForbiddenRequest() {
        String requestId = "test-forbidden-request-id";
        String token = createUserToken();

        webTestClient
                .get()
                .uri("/api/v1/admin/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-Request-Id", requestId)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.FORBIDDEN)
                .expectHeader()
                .contentType("application/json")
                .expectBody()
                .consumeWith(result -> {
                    String body = new String(result.getResponseBody(), StandardCharsets.UTF_8);
                    assertThat(body).contains("\"status\":403");
                    assertThat(body).contains("\"error\":\"Forbidden\"");
                    assertThat(body).contains("\"message\":\"Access to this resource is forbidden\"");
                    assertThat(body).contains("\"path\":\"/api/v1/admin/users\"");
                    assertThat(body).contains("\"requestId\":\"" + requestId + "\"");
                });
    }

    private String createUserToken() {
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