package com.asterion.auth.infrastructure.security;

import com.asterion.auth.application.port.out.PasswordHasher;
import com.asterion.auth.domain.model.PasswordHash;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder =
            new BCryptPasswordEncoder();

    @Override
    public PasswordHash hash(String rawPassword) {
        return new PasswordHash(encoder.encode(rawPassword));
    }

    @Override
    public boolean matches(
            String rawPassword,
            PasswordHash passwordHash
    ) {
        return encoder.matches(
                rawPassword,
                passwordHash.value()
        );
    }
}