package com.asterion.order.infrastructure.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InternalServiceAuthenticationFilterTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private InternalServiceAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new InternalServiceAuthenticationFilter(
                "api-gateway",
                "test-token"
        );
    }

    @Test
    void shouldAcceptTrustedGatewayIdentity() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setRequestURI("/api/v1/orders");
        request.addHeader("X-Service-Name", "api-gateway");
        request.addHeader("X-Service-Token", "test-token");
        request.addHeader("X-User-Id", USER_ID.toString());
        request.addHeader("X-User-Email", "user@example.com");
        request.addHeader("X-User-Roles", "USER");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);

        OrderUserIdentity identity = (OrderUserIdentity) request
                .getAttribute(OrderUserIdentity.class.getName());

        assertNotNull(identity);
        assertEquals(USER_ID, identity.userId());
        assertEquals("user@example.com", identity.email());
        assertEquals("USER", identity.roles());
    }

    @Test
    void shouldRejectInvalidServiceIdentity() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setRequestURI("/api/v1/orders");
        request.addHeader("X-Service-Name", "attacker");
        request.addHeader("X-Service-Token", "wrong");
        request.addHeader("X-User-Id", USER_ID.toString());

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void shouldRejectMissingUserIdentity() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setRequestURI("/api/v1/orders");
        request.addHeader("X-Service-Name", "api-gateway");
        request.addHeader("X-Service-Token", "test-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void shouldRejectInvalidUserId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setRequestURI("/api/v1/orders");
        request.addHeader("X-Service-Name", "api-gateway");
        request.addHeader("X-Service-Token", "test-token");
        request.addHeader("X-User-Id", "not-a-uuid");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain);
    }
}