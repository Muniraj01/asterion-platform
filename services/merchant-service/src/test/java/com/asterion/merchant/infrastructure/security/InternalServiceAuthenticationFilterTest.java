package com.asterion.merchant.infrastructure.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InternalServiceAuthenticationFilterTest {

    private static final String GATEWAY_SERVICE_NAME = "api-gateway";
    private static final String GATEWAY_SERVICE_TOKEN = "test-gateway-service-token";

    private static final String ORDER_SERVICE_NAME = "order-service";
    private static final String ORDER_SERVICE_TOKEN = "test-order-service-token";

    private InternalServiceAuthenticationFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new InternalServiceAuthenticationFilter(
                GATEWAY_SERVICE_NAME,
                GATEWAY_SERVICE_TOKEN,
                ORDER_SERVICE_NAME,
                ORDER_SERVICE_TOKEN
        );

        filterChain = mock(FilterChain.class);
    }

    private MockHttpServletRequest publicMerchantRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/merchants");
        return request;
    }

    private MockHttpServletRequest internalMerchantRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(
                "/internal/api/v1/merchants/11111111-1111-1111-1111-111111111111"
        );
        return request;
    }

    @Test
    void shouldRejectPublicRequestWhenServiceIdentityIsMissing() throws Exception {
        MockHttpServletRequest request = publicMerchantRequest();
        request.addHeader("X-User-Id", "11111111-1111-1111-1111-111111111111");

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldRejectPublicRequestWhenServiceIdentityIsInvalid() throws Exception {
        MockHttpServletRequest request = publicMerchantRequest();

        request.addHeader("X-Service-Name", "attacker");
        request.addHeader("X-Service-Token", "attacker-token");
        request.addHeader("X-User-Id", "11111111-1111-1111-1111-111111111111");

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldRejectPublicRequestWhenUserIdentityIsMissing() throws Exception {
        MockHttpServletRequest request = publicMerchantRequest();

        request.addHeader("X-Service-Name", GATEWAY_SERVICE_NAME);
        request.addHeader("X-Service-Token", GATEWAY_SERVICE_TOKEN);

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldRejectPublicRequestWhenUserIdentityIsInvalid() throws Exception {
        MockHttpServletRequest request = publicMerchantRequest();

        request.addHeader("X-Service-Name", GATEWAY_SERVICE_NAME);
        request.addHeader("X-Service-Token", GATEWAY_SERVICE_TOKEN);
        request.addHeader("X-User-Id", "not-a-uuid");

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldAllowTrustedGatewayRequest() throws Exception {
        UUID userId = UUID.fromString(
                "11111111-1111-1111-1111-111111111111"
        );

        MockHttpServletRequest request = publicMerchantRequest();

        request.addHeader("X-Service-Name", GATEWAY_SERVICE_NAME);
        request.addHeader("X-Service-Token", GATEWAY_SERVICE_TOKEN);
        request.addHeader("X-User-Id", userId.toString());
        request.addHeader("X-User-Email", "owner@example.com");
        request.addHeader("X-User-Roles", "USER");

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);

        MerchantUserIdentity identity = (MerchantUserIdentity) request
                .getAttribute(MerchantUserIdentity.class.getName());

        assertThat(identity).isNotNull();
        assertThat(identity.userId()).isEqualTo(userId);
        assertThat(identity.email()).isEqualTo("owner@example.com");
        assertThat(identity.roles()).isEqualTo("USER");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAllowTrustedOrderServiceRequestToInternalMerchantApi() throws Exception {
        MockHttpServletRequest request = internalMerchantRequest();

        request.addHeader("X-Service-Name", ORDER_SERVICE_NAME);
        request.addHeader("X-Service-Token", ORDER_SERVICE_TOKEN);

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectInternalMerchantRequestWhenOrderServiceIdentityIsInvalid() throws Exception {
        MockHttpServletRequest request = internalMerchantRequest();

        request.addHeader("X-Service-Name", "attacker");
        request.addHeader("X-Service-Token", "attacker-token");

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldRejectInternalMerchantRequestWhenGatewayCredentialsAreUsed() throws Exception {
        MockHttpServletRequest request = internalMerchantRequest();

        request.addHeader("X-Service-Name", GATEWAY_SERVICE_NAME);
        request.addHeader("X-Service-Token", GATEWAY_SERVICE_TOKEN);

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldNotRequireUserIdentityForTrustedOrderServiceRequest() throws Exception {
        MockHttpServletRequest request = internalMerchantRequest();

        request.addHeader("X-Service-Name", ORDER_SERVICE_NAME);
        request.addHeader("X-Service-Token", ORDER_SERVICE_TOKEN);

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(request, response);

        assertThat(request.getAttribute(MerchantUserIdentity.class.getName()))
                .isNull();
    }

    @Test
    void shouldNotFilterNonMerchantRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/actuator/health");

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}