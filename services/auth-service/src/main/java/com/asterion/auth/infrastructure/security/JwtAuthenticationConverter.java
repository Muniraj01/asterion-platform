package com.asterion.auth.infrastructure.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtAuthenticationConverter {

    public Authentication convert(String email) {

        return new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of()
        );
    }
}