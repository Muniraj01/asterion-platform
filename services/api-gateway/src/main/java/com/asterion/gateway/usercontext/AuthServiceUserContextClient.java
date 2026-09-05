package com.asterion.gateway.usercontext;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class AuthServiceUserContextClient implements UserContextClient {

    private final WebClient webClient;
    private final String authServiceBaseUrl;
    private final String serviceName;
    private final String serviceToken;

    public AuthServiceUserContextClient(
            WebClient.Builder webClientBuilder,
            String authServiceBaseUrl,
            String serviceName,
            String serviceToken) {
        this.webClient = webClientBuilder.build();
        this.authServiceBaseUrl = authServiceBaseUrl;
        this.serviceName = serviceName;
        this.serviceToken = serviceToken;
    }

    @Override
    public Mono<UserContext> getUserContext(UUID userId) {
        return webClient
                .get()
                .uri(authServiceBaseUrl + "/api/v1/internal/users/{userId}", userId)
                .header("X-Service-Name", serviceName)
                .header("X-Service-Token", serviceToken)
                .retrieve()
                .bodyToMono(AuthServiceUserResponse.class)
                .map(response -> new UserContext(
                        response.userId(),
                        response.email(),
                        response.active(),
                        response.roles()
                ));
    }

    private record AuthServiceUserResponse(
            UUID userId,
            String email,
            boolean active,
            Instant createdAt,
            Set<String> roles
    ) {
    }
}