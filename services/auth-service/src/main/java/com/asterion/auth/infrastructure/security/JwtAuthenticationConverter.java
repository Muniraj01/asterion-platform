package com.asterion.auth.infrastructure.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtAuthenticationConverter {

    public Authentication convert(
            String email,
            List<String> roles
    ) {
        List<SimpleGrantedAuthority> authorities =
                roles.stream()
                        .map(role -> new SimpleGrantedAuthority(
                                "ROLE_" + role
                        ))
                        .toList();

        return new UsernamePasswordAuthenticationToken(
                email,
                null,
                authorities
        );
    }
}