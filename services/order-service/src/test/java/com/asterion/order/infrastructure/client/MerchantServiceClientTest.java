package com.asterion.order.infrastructure.client;

import com.asterion.order.application.exception.MerchantNotFoundException;
import com.asterion.order.application.exception.MerchantServiceException;
import com.asterion.order.application.model.MerchantValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MerchantServiceClientTest {

    private static final UUID MERCHANT_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private MockRestServiceServer server;
    private MerchantServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();

        server = MockRestServiceServer.bindTo(builder).build();
        client = new MerchantServiceClient(
                builder,
                "http://localhost:8082",
                "order-service",
                "order-token"
        );
    }

    @Test
    void shouldValidateActiveMerchant() {
        server.expect(requestTo(
                "http://localhost:8082/internal/api/v1/merchants/" + MERCHANT_ID))
                .andExpect(method(GET))
                .andExpect(header("X-Service-Name", "order-service"))
                .andExpect(header("X-Service-Token", "order-token"))
                .andRespond(withSuccess(
                                """
                                {
                                  "merchantId":
                                    "22222222-2222-2222-2222-222222222222",
                                  "status": "ACTIVE"
                                }
                                """,
                                APPLICATION_JSON
                        )
                );

        MerchantValidationResult result = client.validate(MERCHANT_ID);

        assertThat(result.merchantId()).isEqualTo(MERCHANT_ID);
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.active()).isTrue();

        server.verify();
    }

    @Test
    void shouldReturnInactiveMerchantStatus() {
        server.expect(requestTo(
                "http://localhost:8082/internal/api/v1/merchants/" + MERCHANT_ID))
                .andRespond(withSuccess(
                                """
                                {
                                  "merchantId":
                                    "22222222-2222-2222-2222-222222222222",
                                  "status": "SUSPENDED"
                                }
                                """,
                                APPLICATION_JSON
                        )
                );

        MerchantValidationResult result = client.validate(MERCHANT_ID);

        assertThat(result.status()).isEqualTo("SUSPENDED");
        assertThat(result.active()).isFalse();

        server.verify();
    }

    @Test
    void shouldMapNotFoundToMerchantNotFoundException() {
        server.expect(requestTo(
                "http://localhost:8082/internal/api/v1/merchants/" + MERCHANT_ID))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(MerchantNotFoundException.class, () -> client.validate(MERCHANT_ID));
        server.verify();
    }

    @Test
    void shouldMapServerErrorToMerchantServiceException() {
        server.expect(requestTo(
                "http://localhost:8082/internal/api/v1/merchants/" + MERCHANT_ID))
                .andRespond(withServerError());

        MerchantServiceException exception = assertThrows(
                MerchantServiceException.class, () -> client.validate(MERCHANT_ID));

        assertThat(exception.getMessage()).contains("Merchant Service returned HTTP");
        server.verify();
    }

    @Test
    void shouldMapConnectionFailureToMerchantServiceException() {
        server.expect(requestTo(
                "http://localhost:8082/internal/api/v1/merchants/" + MERCHANT_ID))
                .andRespond(request -> {
                            throw new IOException("connection failed");}
                );

        MerchantServiceException exception = assertThrows(
                MerchantServiceException.class, () -> client.validate(MERCHANT_ID)
        );

        assertThat(exception.getMessage())
                .isEqualTo("Merchant Service is unavailable");
        assertThat(exception.getCause()).isNotNull();
        server.verify();
    }

    @Test
    void shouldRejectNullMerchantId() {
        assertThrows(IllegalArgumentException.class, () -> client.validate(null));
    }
}