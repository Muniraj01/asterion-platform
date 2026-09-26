package com.asterion.merchant.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class InternalServiceAuthenticationFilter extends OncePerRequestFilter {

    private static final String SERVICE_NAME_HEADER = "X-Service-Name";
    private static final String SERVICE_TOKEN_HEADER = "X-Service-Token";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    private final String expectedServiceName;
    private final String expectedServiceToken;

    public InternalServiceAuthenticationFilter(
            @Value("${asterion.security.internal.service-name}")
            String expectedServiceName,
            @Value("${asterion.security.internal.service-token}")
            String expectedServiceToken) {
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

        String userIdHeader = request.getHeader(USER_ID_HEADER);
        if (userIdHeader == null || userIdHeader.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException exception) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        MerchantUserIdentity identity = new MerchantUserIdentity(
                userId,
                request.getHeader(USER_EMAIL_HEADER),
                request.getHeader(USER_ROLES_HEADER)
        );
        request.setAttribute(MerchantUserIdentity.class.getName(), identity);

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return requestUri == null
                || !(requestUri.equals("/api/v1/merchants")
                || requestUri.startsWith("/api/v1/merchants/"));
    }
}