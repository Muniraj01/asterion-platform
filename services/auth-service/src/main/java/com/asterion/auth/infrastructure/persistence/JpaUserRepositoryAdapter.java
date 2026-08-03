package com.asterion.auth.infrastructure.persistence;

import com.asterion.auth.application.port.out.UserRepository;
import com.asterion.auth.domain.model.EmailAddress;
import com.asterion.auth.domain.model.User;
import com.asterion.auth.domain.model.UserId;
import com.asterion.auth.infrastructure.persistence.mapper.UserEntityMapper;
import com.asterion.auth.infrastructure.persistence.springdata.SpringDataUserJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaUserRepositoryAdapter implements UserRepository {

    private final SpringDataUserJpaRepository repository;

    public JpaUserRepositoryAdapter(
            SpringDataUserJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        var entity = UserEntityMapper.toEntity(user);
        var saved = repository.save(entity);
        return UserEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return repository.findById(id.value())
                .map(UserEntityMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        return repository.findByEmail(email.value())
                .map(UserEntityMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        return repository.existsByEmail(email.value());
    }
}