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
    private static final String PUBLIC_MERCHANT_PREFIX = "/api/v1/merchants";
    private static final String INTERNAL_MERCHANT_PREFIX = "/internal/api/v1/merchants";

    private final String gatewayServiceName;
    private final String gatewayServiceToken;

    private final String orderServiceName;
    private final String orderServiceToken;

    public InternalServiceAuthenticationFilter(
            @Value("${asterion.security.internal.gateway.service-name}")
            String gatewayServiceName,
            @Value("${asterion.security.internal.gateway.service-token}")
            String gatewayServiceToken,
            @Value("${asterion.security.internal.order.service-name}")
            String orderServiceName,
            @Value("${asterion.security.internal.order.service-token}")
            String orderServiceToken) {

        this.gatewayServiceName = gatewayServiceName;
        this.gatewayServiceToken = gatewayServiceToken;
        this.orderServiceName = orderServiceName;
        this.orderServiceToken = orderServiceToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String serviceName = request.getHeader(SERVICE_NAME_HEADER);
        String serviceToken = request.getHeader(SERVICE_TOKEN_HEADER);
        String requestUri = request.getRequestURI();

        // Internal Merchant API
        if (isInternalMerchantRequest(requestUri)) {
            if (!matches(orderServiceName, orderServiceToken, serviceName, serviceToken)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        // Public Merchant API
        if (!matches(gatewayServiceName, gatewayServiceToken, serviceName, serviceToken)) {
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

    private boolean matches(
            String expectedServiceName,
            String expectedServiceToken,
            String actualServiceName,
            String actualServiceToken) {

        return expectedServiceName.equals(actualServiceName)
                && expectedServiceToken.equals(actualServiceToken);
    }

    private boolean isInternalMerchantRequest(String requestUri) {
        return requestUri != null
                && (requestUri.equals(INTERNAL_MERCHANT_PREFIX)
                || requestUri.startsWith(INTERNAL_MERCHANT_PREFIX + "/"));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return requestUri == null ||
                !(requestUri.equals(PUBLIC_MERCHANT_PREFIX)
                        || requestUri.startsWith(PUBLIC_MERCHANT_PREFIX + "/")
                        || requestUri.equals(INTERNAL_MERCHANT_PREFIX)
                        || requestUri.startsWith(INTERNAL_MERCHANT_PREFIX + "/")
                );
    }
}