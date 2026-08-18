package com.asterion.auth.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationConverterTest {

    private final JwtAuthenticationConverter converter =
            new JwtAuthenticationConverter();

    @Test
    void shouldConvertUserRoleToGrantedAuthority() {
        Authentication authentication =
                converter.convert("alice@example.com", List.of("USER"));

        assertEquals("alice@example.com", authentication.getPrincipal());
        assertEquals(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                authentication.getAuthorities()
        );
    }

    @Test
    void shouldConvertAdminRoleToGrantedAuthority() {
        Authentication authentication =
                converter.convert("admin@asterion.com", List.of("ADMIN"));

        assertEquals("admin@asterion.com", authentication.getPrincipal());
        assertEquals(
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                authentication.getAuthorities()
        );
    }

    @Test
    void shouldConvertMultipleRoles() {
        Authentication authentication =
                converter.convert(
                        "admin@asterion.com",
                        List.of("ADMIN", "USER")
                );

        assertEquals("admin@asterion.com", authentication.getPrincipal());

        assertTrue(authentication.getAuthorities().contains(
                new SimpleGrantedAuthority("ROLE_ADMIN")
        ));

        assertTrue(authentication.getAuthorities().contains(
                new SimpleGrantedAuthority("ROLE_USER")
        ));

        assertEquals(2, authentication.getAuthorities().size());
    }

    @Test
    void shouldHandleNoRoles() {
        Authentication authentication =
                converter.convert("alice@example.com", List.of());

        assertEquals("alice@example.com", authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().isEmpty());
    }
}