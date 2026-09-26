package com.asterion.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MerchantGatewayIntegrationTest {

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String ATTACKER_USER_ID = "22222222-2222-2222-2222-222222222222";
    private static MockWebServer mockMerchantService;

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void startMockMerchantService() throws IOException {
        mockMerchantService = new MockWebServer();
        mockMerchantService.start();
    }

    @AfterAll
    static void stopMockMerchantService() throws IOException {
        mockMerchantService.shutdown();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("ASTERION_MERCHANT_SERVICE_URL",
                () -> mockMerchantService.url("/").toString());
    }

    @Test
    void shouldForwardAuthenticatedMerchantCreationWithTrustedIdentity()
            throws InterruptedException, JsonProcessingException {
        mockMerchantService.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "merchantId": "33333333-3333-3333-3333-333333333333",
                          "ownerUserId": "11111111-1111-1111-1111-111111111111",
                          "businessName": "Acme Store",
                          "legalName": "Acme Technologies Pvt Ltd",
                          "contactEmail": "test@example.com",
                          "status": "PENDING",
                          "createdAt": "2026-09-26T10:00:00Z"
                        }
                        """));

        String token = createValidToken(List.of("USER"));

        webTestClient
                .post()
                .uri("/api/v1/merchants")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)

                // Deliberately attacker-controlled headers.
                .header("X-User-Id", ATTACKER_USER_ID)
                .header("X-User-Email", "attacker@example.com")
                .header("X-User-Roles", "ADMIN")
                .header("X-Service-Name", "attacker-service")
                .header("X-Service-Token", "attacker-token")

                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue("""
                        {
                          "businessName": "Acme Store",
                          "legalName": "Acme Technologies Pvt Ltd",
                          "contactEmail": "test@example.com"
                        }
                        """)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CREATED);

        RecordedRequest recordedRequest =
                mockMerchantService.takeRequest(5, TimeUnit.SECONDS);

        assertThat(recordedRequest).isNotNull();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath())
                .isEqualTo("/api/v1/merchants");

        // Gateway must derive the user identity from the JWT.
        assertThat(recordedRequest.getHeader("X-User-Id"))
                .isEqualTo(USER_ID);

        assertThat(recordedRequest.getHeader("X-User-Email"))
                .isEqualTo("test@example.com");

        assertThat(recordedRequest.getHeader("X-User-Roles"))
                .isEqualTo("USER");

        // Gateway must establish its own trusted service identity.
        assertThat(recordedRequest.getHeader("X-Service-Name"))
                .isEqualTo("api-gateway");

        assertThat(recordedRequest.getHeader("X-Service-Token"))
                .isEqualTo("change-me-internal-service-token");

        // Explicitly prove attacker identity did not reach Merchant.
        assertThat(recordedRequest.getHeader("X-User-Id"))
                .isNotEqualTo(ATTACKER_USER_ID);

        assertThat(recordedRequest.getHeader("X-User-Email"))
                .isNotEqualTo("attacker@example.com");

        assertThat(recordedRequest.getHeader("X-User-Roles"))
                .isNotEqualTo("ADMIN");

        assertThat(recordedRequest.getHeader("X-Service-Name"))
                .isNotEqualTo("attacker-service");

        assertThat(recordedRequest.getHeader("X-Service-Token"))
                .isNotEqualTo("attacker-token");

        String body = recordedRequest.getBody().readUtf8();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode requestJson = objectMapper.readTree(body);

        assertThat(requestJson.get("businessName").asText())
                .isEqualTo("Acme Store");
        assertThat(requestJson.get("legalName").asText())
                .isEqualTo("Acme Technologies Pvt Ltd");
        assertThat(requestJson.get("contactEmail").asText())
                .isEqualTo("test@example.com");
    }

    private String createValidToken(List<String> roles) {
        var key = Keys.hmacShaKeyFor("change-me-change-me-change-me-change-me"
                .getBytes(StandardCharsets.UTF_8));

        Instant now = Instant.now();
        return Jwts.builder()
                .subject(USER_ID)
                .claim("email", "test@example.com")
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(key)
                .compact();
    }
}