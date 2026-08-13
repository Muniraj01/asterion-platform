package com.asterion.auth.infrastructure.persistence;

import com.asterion.auth.application.port.out.RefreshTokenRepository;
import com.asterion.auth.domain.model.RefreshToken;
import com.asterion.auth.domain.model.RefreshTokenId;
import com.asterion.auth.infrastructure.persistence.mapper.RefreshTokenEntityMapper;
import com.asterion.auth.infrastructure.persistence.springdata.SpringDataRefreshTokenJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaRefreshTokenRepositoryAdapter
        implements RefreshTokenRepository {

    private final SpringDataRefreshTokenJpaRepository repository;

    public JpaRefreshTokenRepositoryAdapter(
            SpringDataRefreshTokenJpaRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {

        var entity = RefreshTokenEntityMapper.toEntity(refreshToken);

        var saved = repository.save(entity);

        return RefreshTokenEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findById(RefreshTokenId id) {

        return repository.findById(id.value())
                .map(RefreshTokenEntityMapper::toDomain);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {

        return repository.findByTokenHash(tokenHash)
                .map(RefreshTokenEntityMapper::toDomain);
    }
}