package com.asterion.auth.infrastructure.persistence.springdata;

import com.asterion.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataRefreshTokenJpaRepository
        extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
        update RefreshTokenEntity t
           set t.revokedAt = :revokedAt
         where t.userId = :userId
           and t.revokedAt is null
        """)
    void revokeAllByUserId(
            @Param("userId") UUID userId,
            @Param("revokedAt") Instant revokedAt
    );

    default void revokeAllByUserId(UUID userId) {
        revokeAllByUserId(userId, Instant.now());
    }

    @Modifying
    @Query("""
    update RefreshTokenEntity t
       set t.revokedAt = :revokedAt
     where t.familyId = :familyId
       and t.revokedAt is null
    """)
    void revokeFamily(
            @Param("familyId") UUID familyId,
            @Param("revokedAt") Instant revokedAt
    );

    default void revokeFamily(UUID familyId) {
        revokeFamily(familyId, Instant.now());
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select t
      from RefreshTokenEntity t
     where t.tokenHash = :tokenHash
    """)
    Optional<RefreshTokenEntity> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );
}