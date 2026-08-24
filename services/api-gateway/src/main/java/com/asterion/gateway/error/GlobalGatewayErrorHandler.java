package com.asterion.gateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import io.netty.channel.AbstractChannel;
import java.net.ConnectException;
import java.time.Instant;
import java.util.UUID;

@Component
@Order(-2)
public class GlobalGatewayErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalGatewayErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable throwable) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(throwable);
        }

        String requestId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        HttpStatus status = determineStatus(throwable);
        GatewayErrorResponse errorResponse = new GatewayErrorResponse(
                Instant.now(), 
                status.value(), 
                status.getReasonPhrase(), 
                messageFor(status), 
                exchange.getRequest().getPath().value(), requestId
        );

        byte[] responseBytes;
        try {
            responseBytes = objectMapper.writeValueAsBytes(errorResponse);
        } catch (Exception serializationException) {
            return Mono.error(serializationException);
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = response.bufferFactory().wrap(responseBytes);
        return response.writeWith(Mono.just(buffer));
    }

    private HttpStatus determineStatus(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ResponseStatusException respStatusException) {
                return HttpStatus.valueOf(respStatusException.getStatusCode().value());
            }

            if (current instanceof ConnectException) {
                return HttpStatus.SERVICE_UNAVAILABLE;
            }
            current = current.getCause();
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String messageFor(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "The request is invalid";
            case UNAUTHORIZED -> "Authentication is required";
            case FORBIDDEN -> "Access to this resource is forbidden";
            case NOT_FOUND -> "Resource not found";
            case INTERNAL_SERVER_ERROR -> "An unexpected error occurred";
            default -> status.getReasonPhrase();
        };
    }
}