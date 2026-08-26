package com.asterion.gateway.fallback;

import com.asterion.gateway.error.GatewayErrorResponse;
import com.asterion.gateway.error.GatewayErrorResponseWriter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import java.util.concurrent.TimeoutException;

@RestController
public class AuthServiceFallbackHandler {

    private final GatewayErrorResponseWriter responseWriter;

    public AuthServiceFallbackHandler(GatewayErrorResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @RequestMapping("/fallback/auth-service")
    public Mono<Void> fallback(ServerWebExchange exchange) {

        String requestId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Request-Id");

        Throwable failure = exchange.getAttribute(
                ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);

        HttpStatus status = determineStatus(failure);

        GatewayErrorResponse response = new GatewayErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                messageFor(status),
                exchange.getRequest().getPath().value(),
                requestId
        );

        return responseWriter.write(exchange, response);
    }

    private HttpStatus determineStatus(Throwable failure) {
        if (containsCause(failure, TimeoutException.class)) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }

        return HttpStatus.SERVICE_UNAVAILABLE;
    }

    private String messageFor(HttpStatus status) {
        return switch (status) {
            case GATEWAY_TIMEOUT -> "Auth service request timed out";
            case SERVICE_UNAVAILABLE -> "Auth service is temporarily unavailable";
            default -> status.getReasonPhrase();
        };
    }

    private boolean containsCause(Throwable throwable,
                                  Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}