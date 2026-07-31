package com.asterion.auth.infrastructure.persistence;

import com.asterion.auth.application.port.out.UserRepository;
import com.asterion.auth.domain.model.EmailAddress;
import com.asterion.auth.domain.model.User;
import com.asterion.auth.domain.model.UserId;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<UserId, User> users = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        users.put(user.id(), user);
        return user;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        return users.values().stream()
                .filter(user -> user.email().equals(email))
                .findFirst();
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        return users.values().stream()
                .anyMatch(user -> user.email().equals(email));
    }
}