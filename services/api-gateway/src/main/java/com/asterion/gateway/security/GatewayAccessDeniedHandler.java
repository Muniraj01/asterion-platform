package com.asterion.gateway.security;

import com.asterion.gateway.error.GatewayErrorResponse;
import com.asterion.gateway.error.GatewayErrorResponseWriter;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class GatewayAccessDeniedHandler implements ServerAccessDeniedHandler {

    private final GatewayErrorResponseWriter responseWriter;

    public GatewayAccessDeniedHandler(GatewayErrorResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException exception) {
        String requestId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Request-Id");

        GatewayErrorResponse response = new GatewayErrorResponse(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "Access to this resource is forbidden",
                exchange.getRequest().getPath().value(), requestId
        );

        return responseWriter.write(exchange, response);
    }
}