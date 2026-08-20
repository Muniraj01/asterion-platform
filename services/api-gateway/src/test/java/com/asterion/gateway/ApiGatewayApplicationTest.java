package com.asterion.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ApiGatewayApplicationTest {

    private static final WireMockServer authService =
            new WireMockServer(0);

    private static final String JWT_SECRET =
            "change-me-change-me-change-me-change-me";

    @LocalServerPort
    private int gatewayPort;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @BeforeAll
    static void startAuthService() {
        authService.start();
    }

    @AfterAll
    static void stopAuthService() {
        authService.stop();
    }

    @DynamicPropertySource
    static void gatewayProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "ASTERION_AUTH_SERVICE_URL",
                authService::baseUrl
        );
    }

    @Test
    void shouldRouteUserRequestToAuthService() {

        authService.stubFor(
                get(urlEqualTo("/api/v1/users/me"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody("""
                                                {
                                                  "email": "test@example.com",
                                                  "authenticated": true
                                                }
                                                """)
                        )
        );

        String token = createValidUserToken();

        String response =
                webClientBuilder
                        .baseUrl("http://localhost:" + gatewayPort)
                        .build()
                        .get()
                        .uri("/api/v1/users/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

        assertThat(response)
                .contains("test@example.com");

        authService.verify(
                getRequestedFor(
                        urlEqualTo("/api/v1/users/me")
                )
        );
    }

    private String createValidUserToken() {

        SecretKey key = Keys.hmacShaKeyFor(
                JWT_SECRET.getBytes(StandardCharsets.UTF_8)
        );

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