package com.asterion.gateway.security;

import com.asterion.gateway.error.GatewayErrorResponse;
import com.asterion.gateway.error.GatewayErrorResponseWriter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class GatewayAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final GatewayErrorResponseWriter responseWriter;

    public GatewayAuthenticationEntryPoint(GatewayErrorResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException exception) {
        String requestId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Request-Id");

        GatewayErrorResponse response = new GatewayErrorResponse(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Authentication is required",
                exchange.getRequest().getPath().value(), requestId
        );

        return responseWriter.write(exchange, response);
    }
}