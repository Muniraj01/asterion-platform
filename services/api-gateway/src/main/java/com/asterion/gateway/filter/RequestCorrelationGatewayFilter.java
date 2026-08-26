package com.asterion.gateway.security;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class RequestCorrelationGatewayFilter implements GlobalFilter, Ordered {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_CONTEXT_KEY = "asterion.requestId";
    public static final String ORIGINAL_REQUEST_PATH = "asterion.originalRequestPath";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);

        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        String finalRequestId = requestId;
        String originalRequestPath = exchange.getRequest().getPath().value();

        ServerWebExchange mutatedExchange = exchange
                .mutate()
                .request(request -> request.headers(headers -> {
                    headers.remove(REQUEST_ID_HEADER);
                    headers.add(REQUEST_ID_HEADER, finalRequestId);
                }))
                .build();

        mutatedExchange.getAttributes()
                .put(ORIGINAL_REQUEST_PATH, originalRequestPath);

        mutatedExchange.getResponse()
                .getHeaders()
                .set(REQUEST_ID_HEADER, finalRequestId);

        return chain
                .filter(mutatedExchange)
                .contextWrite(context ->
                        context.put(REQUEST_ID_CONTEXT_KEY, finalRequestId))
                .doOnEach(signal -> {
                    if (!signal.isOnNext() && !signal.isOnComplete()) {
                        return;
                    }
                    signal.getContextView()
                            .<String>getOrEmpty(REQUEST_ID_CONTEXT_KEY)
                            .ifPresent(id -> MDC.put(REQUEST_ID_HEADER, id));
                })
                .doFinally(signal -> MDC.remove(REQUEST_ID_HEADER));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}