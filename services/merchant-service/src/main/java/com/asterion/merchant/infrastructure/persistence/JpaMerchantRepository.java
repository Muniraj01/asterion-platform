package com.asterion.merchant.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaMerchantRepository
        extends JpaRepository<MerchantJpaEntity, UUID> {
}