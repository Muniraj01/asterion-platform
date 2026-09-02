package com.asterion.auth.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InternalServiceAuthenticationFilterTest {

    private static final String SERVICE_NAME = "api-gateway";
    private static final String SERVICE_TOKEN = "test-internal-service-token";
    private InternalServiceAuthenticationFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new InternalServiceAuthenticationFilter(SERVICE_NAME, SERVICE_TOKEN);
        filterChain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest internalRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/internal/test");
        return request;
    }

    @Test
    void shouldRejectRequestWhenServiceIdentityIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = internalRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldRejectRequestWhenServiceIdentityIsInvalid() throws ServletException, IOException {
        MockHttpServletRequest request = internalRequest();
        request.addHeader("X-Service-Name", "attacker-service");
        request.addHeader("X-Service-Token", "invalid-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldAllowRequestWhenServiceIdentityIsValid() throws ServletException, IOException {
        MockHttpServletRequest request = internalRequest();
        request.addHeader("X-Service-Name", SERVICE_NAME);
        request.addHeader("X-Service-Token", SERVICE_TOKEN);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectRequestWhenServiceNameIsValidButTokenIsInvalid()
            throws ServletException, IOException {
        MockHttpServletRequest request = internalRequest();
        request.addHeader("X-Service-Name", SERVICE_NAME);
        request.addHeader("X-Service-Token", "wrong-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldRejectRequestWhenServiceTokenIsValidButServiceNameIsInvalid()
            throws ServletException, IOException {
        MockHttpServletRequest request = internalRequest();
        request.addHeader("X-Service-Name", "unknown-service");
        request.addHeader("X-Service-Token", SERVICE_TOKEN);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(filterChain);
    }

    @Test
    void shouldAuthenticateServiceWhenServiceIdentityIsValid()
            throws ServletException, IOException {
        MockHttpServletRequest request = internalRequest();
        request.addHeader("X-Service-Name", SERVICE_NAME);
        request.addHeader("X-Service-Token", SERVICE_TOKEN);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isEqualTo(SERVICE_NAME);
    }

    @Test
    void shouldAuthenticateServiceWithServiceAuthority() throws ServletException, IOException {
        MockHttpServletRequest request = internalRequest();
        request.addHeader("X-Service-Name", SERVICE_NAME);
        request.addHeader("X-Service-Token", SERVICE_TOKEN);

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(SERVICE_NAME);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_SERVICE");
    }
}