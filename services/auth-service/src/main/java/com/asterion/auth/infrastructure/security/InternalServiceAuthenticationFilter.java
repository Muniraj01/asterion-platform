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

    private final InternalServiceProperties properties;

    public InternalServiceAuthenticationFilter(InternalServiceProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String serviceName = request.getHeader(SERVICE_NAME_HEADER);
        String serviceToken = request.getHeader(SERVICE_TOKEN_HEADER);

        if (serviceName == null || serviceToken == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        InternalServiceProperties.Service service = properties.services().get(serviceName);
        if (service == null || !service.token().equals(serviceToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        InternalServiceAuthentication authentication =
                new InternalServiceAuthentication(serviceName, service.permissions());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return requestUri == null || !requestUri.startsWith("/api/v1/internal/");
    }
}