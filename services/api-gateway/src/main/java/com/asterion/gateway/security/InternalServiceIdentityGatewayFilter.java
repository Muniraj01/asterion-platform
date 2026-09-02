package com.asterion.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;

@Component
public class InternalServiceIdentityGatewayFilter implements GlobalFilter, Ordered {

    private static final String SERVICE_NAME_HEADER = "X-Service-Name";
    private static final String SERVICE_TOKEN_HEADER = "X-Service-Token";

    private final String serviceName;
    private final String serviceToken;

    public InternalServiceIdentityGatewayFilter(
            @Value("${asterion.security.internal.service-name}") String serviceName,
            @Value("${asterion.security.internal.service-token}") String serviceToken) {
        this.serviceName = serviceName;
        this.serviceToken = serviceToken;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest mutatedRequest = exchange
                .getRequest()
                .mutate()
                .headers(headers -> {
                    // Never trust client-supplied service identity.
                    headers.remove(SERVICE_NAME_HEADER);
                    headers.remove(SERVICE_TOKEN_HEADER);

                    headers.add(SERVICE_NAME_HEADER, serviceName);
                    headers.add(SERVICE_TOKEN_HEADER, serviceToken);
                })
                .build();

        return chain.filter(exchange
                .mutate()
                .request(mutatedRequest)
                .build());
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }
}