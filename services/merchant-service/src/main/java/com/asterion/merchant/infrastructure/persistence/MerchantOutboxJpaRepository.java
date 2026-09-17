package com.asterion.merchant.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MerchantOutboxJpaRepository
        extends JpaRepository<MerchantOutboxJpaEntity, UUID> {

    List<MerchantOutboxJpaEntity> findByStatusOrderByCreatedAtAsc(
            String status, Pageable pageable);
}
