package com.asterion.order.infrastructure.client;

import com.asterion.order.application.exception.MerchantNotFoundException;
import com.asterion.order.application.exception.MerchantServiceException;
import com.asterion.order.application.model.MerchantValidationResult;
import com.asterion.order.application.port.out.MerchantValidationPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class MerchantServiceClient implements MerchantValidationPort {

    private static final String SERVICE_NAME_HEADER = "X-Service-Name";
    private static final String SERVICE_TOKEN_HEADER = "X-Service-Token";

    private final RestClient restClient;
    private final String serviceName;
    private final String serviceToken;

    public MerchantServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${asterion.merchant-service.url}")
            String merchantServiceUrl,
            @Value("${asterion.security.internal.service-name}")
            String serviceName,
            @Value("${asterion.security.internal.service-token}")
            String serviceToken) {

        this.restClient = restClientBuilder.baseUrl(merchantServiceUrl).build();
        this.serviceName = serviceName;
        this.serviceToken = serviceToken;
    }

    @Override
    public MerchantValidationResult validate(UUID merchantId) {
        if (merchantId == null)
            throw new IllegalArgumentException("merchantId must not be null");

        try {
            MerchantValidationResult result = restClient
                    .get()
                    .uri("/internal/api/v1/merchants/{merchantId}", merchantId)
                    .header(SERVICE_NAME_HEADER, serviceName)
                    .header(SERVICE_TOKEN_HEADER, serviceToken)
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            (request, response) -> {
                                throw new MerchantNotFoundException(merchantId);
                            }
                    )
                    .onStatus(HttpStatusCode::isError,
                            (request, response) -> {
                                throw new MerchantServiceException(
                                        "Merchant Service returned HTTP "
                                                + response.getStatusCode().value());
                            }
                    )
                    .body(MerchantValidationResult.class);

            if (result == null)
                throw new MerchantServiceException("Merchant Service returned an empty response");

            return result;

        } catch (MerchantNotFoundException | MerchantServiceException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new MerchantServiceException("Merchant Service is unavailable", exception);
        }
    }
}