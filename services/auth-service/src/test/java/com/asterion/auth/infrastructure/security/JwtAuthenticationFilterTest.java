package com.asterion.auth.infrastructure.security;

import com.asterion.auth.application.port.out.JwtTokenProvider;
import com.asterion.auth.application.port.out.PasswordHasher;
import com.asterion.auth.application.port.out.UserRepository;
import com.asterion.auth.domain.model.EmailAddress;
import com.asterion.auth.domain.model.PasswordHash;
import com.asterion.auth.domain.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtAuthenticationConverter converter;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Authentication authentication;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(
                jwtTokenProvider,
                converter,
                userRepository
        );

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateActiveUserWithValidToken() throws Exception {
        User user = activeUser();

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer valid-token");
        when(jwtTokenProvider.isValid("valid-token"))
                .thenReturn(true);
        when(jwtTokenProvider.extractEmail("valid-token"))
                .thenReturn("test@example.com");
        when(jwtTokenProvider.extractRoles("valid-token"))
                .thenReturn(List.of("USER"));
        when(userRepository.findByEmail(any(EmailAddress.class)))
                .thenReturn(Optional.of(user));
        when(converter.convert("test@example.com", List.of("USER")))
                .thenReturn(authentication);

        filter.doFilterInternal(request, response, filterChain);

        assertSame(
                authentication,
                SecurityContextHolder.getContext().getAuthentication()
        );

        verify(converter).convert(
                "test@example.com",
                List.of("USER")
        );
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectInactiveUserWithValidToken() throws Exception {
        User user = activeUser();
        user.deactivate();

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer valid-token");
        when(jwtTokenProvider.isValid("valid-token"))
                .thenReturn(true);
        when(jwtTokenProvider.extractEmail("valid-token"))
                .thenReturn("test@example.com");
        when(userRepository.findByEmail(any(EmailAddress.class)))
                .thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(converter);
        assertSame(
                null,
                SecurityContextHolder.getContext().getAuthentication()
        );
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectInvalidToken() throws Exception {
        String token = "invalid-token";

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer " + token);

        when(jwtTokenProvider.isValid(token))
                .thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verify(request).getHeader("Authorization");
        verify(jwtTokenProvider).isValid(token);
        verifyNoMoreInteractions(jwtTokenProvider);

        verifyNoInteractions(userRepository, converter);
        verify(filterChain).doFilter(request, response);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldIgnoreRequestWithoutBearerToken() throws Exception {
        when(request.getHeader("Authorization"))
                .thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(
                jwtTokenProvider,
                userRepository,
                converter
        );

        assertSame(
                null,
                SecurityContextHolder.getContext().getAuthentication()
        );
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectValidTokenWhenUserDoesNotExist() throws Exception {
        when(request.getHeader("Authorization"))
                .thenReturn("Bearer valid-token");
        when(jwtTokenProvider.isValid("valid-token"))
                .thenReturn(true);
        when(jwtTokenProvider.extractEmail("valid-token"))
                .thenReturn("unknown@example.com");
        when(userRepository.findByEmail(any(EmailAddress.class)))
                .thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(converter);
        assertSame(
                null,
                SecurityContextHolder.getContext().getAuthentication()
        );
        verify(filterChain).doFilter(request, response);
    }

    private User activeUser() {
        return User.register(
                new EmailAddress("test@example.com"),
                new PasswordHash("encoded-password")
        );
    }
}