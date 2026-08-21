package com.asterion.gateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public GatewayErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(ServerWebExchange exchange,
                            GatewayErrorResponse errorResponse) {
        try {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatusCode.valueOf(errorResponse.status()));
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            byte[] responseBytes = objectMapper.writeValueAsBytes(errorResponse);
            var buffer = response.bufferFactory().wrap(responseBytes);

            return response.writeWith(Mono.just(buffer));
        } catch (Exception ex) {
            return Mono.error(ex);
        }
    }
}