package com.asterion.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            GatewayAuthenticationEntryPoint gatewayAuthenticationEntryPoint,
            GatewayAccessDeniedHandler gatewayAccessDeniedHandler) {

        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/refresh"
                        ).permitAll()

                        .pathMatchers("/api/v1/admin/**")
                        .hasRole("ADMIN")

                        .pathMatchers("/api/v1/users/**")
                        .hasAnyRole("USER", "ADMIN")

                        .anyExchange()
                        .authenticated()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(gatewayAuthenticationEntryPoint)
                        .accessDeniedHandler(gatewayAccessDeniedHandler)
                        .jwt(jwt -> {}))
                .build();
    }
}