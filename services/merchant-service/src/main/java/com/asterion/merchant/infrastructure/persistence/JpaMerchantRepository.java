package com.asterion.merchant.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaMerchantRepository
        extends JpaRepository<MerchantJpaEntity, UUID> {

    Page<MerchantJpaEntity> findByOwnerUserId(
            UUID ownerUserId,
            Pageable pageable
    );
}