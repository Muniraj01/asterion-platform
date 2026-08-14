package com.asterion.auth.infrastructure.persistence;

import com.asterion.auth.application.port.out.RefreshTokenRepository;
import com.asterion.auth.domain.model.RefreshToken;
import com.asterion.auth.domain.model.RefreshTokenId;
import com.asterion.auth.domain.model.UserId;
import com.asterion.auth.infrastructure.persistence.mapper.RefreshTokenEntityMapper;
import com.asterion.auth.infrastructure.persistence.springdata.SpringDataRefreshTokenJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaRefreshTokenRepositoryAdapter
        implements RefreshTokenRepository {

    private final SpringDataRefreshTokenJpaRepository repository;

    public JpaRefreshTokenRepositoryAdapter(SpringDataRefreshTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        var entity = RefreshTokenEntityMapper.toEntity(token);
        var saved = repository.save(entity);
        return RefreshTokenEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findById(RefreshTokenId id) {
        return repository.findById(id.value()).map(RefreshTokenEntityMapper::toDomain);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(RefreshTokenEntityMapper::toDomain);
    }

    @Override
    public void revoke(RefreshToken token) {
        repository.save(RefreshTokenEntityMapper.toEntity(token));
    }

    @Override
    public void revokeAllByUserId(UserId userId) {
        repository.revokeAllByUserId(userId.value());
    }
}