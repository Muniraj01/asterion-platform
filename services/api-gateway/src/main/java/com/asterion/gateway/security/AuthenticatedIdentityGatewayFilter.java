package com.asterion.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticatedIdentityGatewayFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        return exchange.getPrincipal()
                .cast(Authentication.class)
                .filter(Authentication::isAuthenticated)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .flatMap(jwt -> {

                    String userId = jwt.getSubject();
                    String email = jwt.getClaimAsString("email");

                    var roles = jwt.getClaimAsStringList("roles");

                    String rolesHeader = roles == null ? "" : String.join(",", roles);

                    var mutatedRequest = exchange
                            .getRequest()
                            .mutate()
                            .headers(headers -> {
                                headers.remove(USER_ID_HEADER);
                                headers.remove(USER_EMAIL_HEADER);
                                headers.remove(USER_ROLES_HEADER);

                                headers.add(USER_ID_HEADER, userId);

                                if (email != null) {
                                    headers.add(USER_EMAIL_HEADER, email);
                                }

                                if (!rolesHeader.isBlank()) {
                                    headers.add(USER_ROLES_HEADER, rolesHeader);
                                }
                            })
                            .build();

                    return chain.filter(
                            exchange.mutate().request(mutatedRequest).build());
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}