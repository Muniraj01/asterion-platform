package com.asterion.gateway.usercontext;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserContextClient {

    Mono<UserContext> getUserContext(UUID userId);
}