package com.asterion.merchant.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfiguration {

    private final InternalServiceAuthenticationFilter internalServiceAuthenticationFilter;

    public SecurityConfiguration(
            InternalServiceAuthenticationFilter internalServiceAuthenticationFilter) {
        this.internalServiceAuthenticationFilter = internalServiceAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .exceptionHandling(exceptionHandling ->
                        exceptionHandling.authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info")
                        .permitAll()

                        .requestMatchers("/api/v1/merchants/**")
                        .permitAll()

                        .anyRequest()
                        .authenticated()
                )

                .addFilterBefore(internalServiceAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}