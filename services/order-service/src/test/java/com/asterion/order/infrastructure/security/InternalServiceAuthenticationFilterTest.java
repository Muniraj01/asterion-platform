package com.asterion.order.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InternalServiceAuthenticationFilterTest {

    private static final String SERVICE_NAME = "order-service";
    private static final String SERVICE_TOKEN = "test-order-service-token";

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private InternalServiceAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new InternalServiceAuthenticationFilter(SERVICE_NAME, SERVICE_TOKEN);
    }

    @Test
    void shouldAcceptTrustedOrderServiceRequestWithValidUserIdentity()
            throws ServletException, IOException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/orders");

        request.addHeader("X-Service-Name", SERVICE_NAME);
        request.addHeader("X-Service-Token", SERVICE_TOKEN);
        request.addHeader("X-User-Id", USER_ID.toString());
        request.addHeader("X-User-Email", "user@asterion.com");
        request.addHeader("X-User-Roles", "USER");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());

        OrderUserIdentity identity = (OrderUserIdentity) request
                .getAttribute(OrderUserIdentity.class.getName());

        assertNotNull(identity);
        assertEquals(USER_ID, identity.userId());
        assertEquals("user@asterion.com", identity.email());
        assertEquals("USER", identity.roles());
    }

    @Test
    void shouldRejectMissingServiceIdentity() throws ServletException, IOException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/orders");
        request.addHeader("X-User-Id", USER_ID.toString());

        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void shouldRejectInvalidServiceName() throws ServletException, IOException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/orders");

        request.addHeader("X-Service-Name", "wrong-service");
        request.addHeader("X-Service-Token", SERVICE_TOKEN);
        request.addHeader("X-User-Id", USER_ID.toString());

        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void shouldRejectInvalidServiceToken() throws ServletException, IOException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/orders");

        request.addHeader("X-Service-Name", SERVICE_NAME);
        request.addHeader("X-Service-Token", "wrong-token");
        request.addHeader("X-User-Id", USER_ID.toString());

        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void shouldRejectMissingUserIdentity() throws ServletException, IOException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/orders");

        request.addHeader("X-Service-Name", SERVICE_NAME);
        request.addHeader("X-Service-Token", SERVICE_TOKEN);

        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void shouldRejectInvalidUserId() throws ServletException, IOException {

        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/orders");

        request.addHeader("X-Service-Name", SERVICE_NAME);
        request.addHeader("X-Service-Token", SERVICE_TOKEN);
        request.addHeader("X-User-Id", "not-a-uuid");

        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void shouldAcceptNestedOrderPath() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/v1/orders/" + UUID.randomUUID()
        );

        request.addHeader("X-Service-Name", SERVICE_NAME);
        request.addHeader("X-Service-Token", SERVICE_TOKEN);
        request.addHeader("X-User-Id", USER_ID.toString());

        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(request.getAttribute(OrderUserIdentity.class.getName()));
    }

    @Test
    void shouldBypassNonOrderRequest() throws ServletException, IOException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/actuator/health");

        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
        assertNull(request.getAttribute(OrderUserIdentity.class.getName()));
    }

    @Test
    void shouldBypassDifferentApiPath() throws ServletException, IOException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/merchants");

        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
        assertNull(request.getAttribute(OrderUserIdentity.class.getName()));
    }
}