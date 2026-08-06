package com.asterion.auth.application.port.out;

import com.asterion.auth.domain.model.PasswordHash;

public interface PasswordHasher {

    PasswordHash hash(String rawPassword);

    boolean matches(String rawPassword, PasswordHash passwordHash);
}