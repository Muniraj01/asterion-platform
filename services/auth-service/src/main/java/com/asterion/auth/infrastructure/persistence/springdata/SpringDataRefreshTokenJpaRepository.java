package com.asterion.auth.infrastructure.persistence.springdata;

import com.asterion.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataRefreshTokenJpaRepository
        extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);
}