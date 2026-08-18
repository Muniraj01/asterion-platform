package com.asterion.auth.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class User {

    private final UserId id;
    private final EmailAddress email;
    private final PasswordHash passwordHash;
    private final Instant createdAt;
    private final Set<Role> roles;
    private boolean active;

    public User(
            UserId id,
            EmailAddress email,
            PasswordHash passwordHash,
            Instant createdAt,
            boolean active,
            Set<Role> roles
    ) {
        this.id = Objects.requireNonNull(id);
        this.email = Objects.requireNonNull(email);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.active = active;
        this.roles = roles == null
                ? EnumSet.noneOf(Role.class)
                : EnumSet.copyOf(roles);
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
                true,
                EnumSet.of(Role.USER)
        );
    }

    public boolean hasRole(Role role) {
        return roles.contains(Objects.requireNonNull(role));
    }

    public void addRole(Role role) {
        roles.add(Objects.requireNonNull(role));
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

    public Set<Role> roles() {
        return Collections.unmodifiableSet(roles);
    }

    public void deactivate() {
        this.active = false;
    }
}