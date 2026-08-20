package com.asterion.gateway.observability;

import org.slf4j.MDC;
import reactor.core.publisher.Mono;

public final class CorrelationContext {

    public static final String REQUEST_ID = "asterion.requestId";

    public static final String REQUEST_ID_MDC = "requestId";

    private CorrelationContext() {}

    public static <T> Mono<T> withRequestId(String requestId, Mono<T> publisher) {
        return publisher.contextWrite(context ->
                context.put(REQUEST_ID, requestId)
        );
    }

    public static void putMdc(String requestId) {
        MDC.put(REQUEST_ID_MDC, requestId);
    }

    public static void clearMdc() {
        MDC.remove(REQUEST_ID_MDC);
    }
}