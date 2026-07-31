package com.asterion.auth.domain.model;

import java.time.Instant;
import java.util.Objects;

public final class User {

    private final UserId id;
    private final EmailAddress email;
    private final PasswordHash passwordHash;
    private final Instant createdAt;
    private boolean active;

    private User(
            UserId id,
            EmailAddress email,
            PasswordHash passwordHash,
            Instant createdAt,
            boolean active
    ) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.active = active;
    }

    public static User register(
            EmailAddress email,
            PasswordHash passwordHash
    ) {
        return new User(
                UserId.newId(),
                email,
                passwordHash,
                Instant.now(),
                true
        );
    }

    public UserId id() {
        return id;
    }

    public EmailAddress email() {
        return email;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }
}