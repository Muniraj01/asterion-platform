package com.asterion.auth.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;

@Component
public class InternalServiceAuthenticationFilter extends OncePerRequestFilter {

    private static final String SERVICE_NAME_HEADER = "X-Service-Name";
    private static final String SERVICE_TOKEN_HEADER = "X-Service-Token";

    private final String expectedServiceName;
    private final String expectedServiceToken;

    public InternalServiceAuthenticationFilter(
            @Value("${asterion.security.internal.service-name}") String expectedServiceName,
            @Value("${asterion.security.internal.service-token}") String expectedServiceToken) {
        this.expectedServiceName = expectedServiceName;
        this.expectedServiceToken = expectedServiceToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String serviceName = request.getHeader(SERVICE_NAME_HEADER);
        String serviceToken = request.getHeader(SERVICE_TOKEN_HEADER);
        if (!expectedServiceName.equals(serviceName)
                || !expectedServiceToken.equals(serviceToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        InternalServiceAuthentication authentication = new InternalServiceAuthentication(serviceName);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/api/v1/internal/");
    }
}