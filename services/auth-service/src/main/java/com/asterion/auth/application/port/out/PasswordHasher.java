package com.asterion.auth.application.port.out;

import org.springframework.stereotype.Component;

/**
 * Application port for password hashing operations.
 *
 * The domain model must not depend on a concrete hashing
 * implementation such as BCrypt or Argon2.
 */
public interface PasswordHasher {

    /**
     * Hash a raw password.
     *
     * @param rawPassword plain text password
     * @return hashed password representation
     */
    String hash(String rawPassword);

    /**
     * Verify whether a raw password matches a stored hash.
     *
     * @param rawPassword plain text password
     * @param hashedPassword stored hash
     * @return true if the password matches
     */
    boolean matches(String rawPassword, String hashedPassword);
}