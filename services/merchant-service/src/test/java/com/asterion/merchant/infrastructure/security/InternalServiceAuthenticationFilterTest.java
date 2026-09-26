package com.asterion.merchant.infrastructure.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InternalServiceAuthenticationFilterTest {

    private static final String SERVICE_NAME = "api-gateway";
    private static final String SERVICE_TOKEN = "test-internal-service-token";

    private InternalServiceAuthenticationFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new InternalServiceAuthenticationFilter(SERVICE_NAME, SERVICE_TOKEN);
        filterChain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/merchants");
        return request;
    }

    @Test
    void shouldRejectRequestWhenServiceIdentityIsMissing() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader("X-User-Id", "11111111-1111-1111-1111-111111111111");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldRejectRequestWhenServiceIdentityIsInvalid() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader("X-Service-Name", "attacker");
        request.addHeader("X-Service-Token", "attacker-token");
        request.addHeader("X-User-Id", "11111111-1111-1111-1111-111111111111");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldRejectRequestWhenUserIdentityIsMissing() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader("X-Service-Name", SERVICE_NAME);
        request.addHeader("X-Service-Token", SERVICE_TOKEN);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldRejectRequestWhenUserIdentityIsInvalid() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader("X-Service-Name", SERVICE_NAME);
        request.addHeader("X-Service-Token", SERVICE_TOKEN);
        request.addHeader("X-User-Id", "not-a-uuid");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldAllowTrustedGatewayRequest() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        MockHttpServletRequest request = request();

        request.addHeader("X-Service-Name", SERVICE_NAME);
        request.addHeader("X-Service-Token", SERVICE_TOKEN);
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
    void shouldNotFilterNonMerchantRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/actuator/health");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}