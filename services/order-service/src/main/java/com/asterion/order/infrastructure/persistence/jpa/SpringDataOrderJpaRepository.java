package com.asterion.order.infrastructure.persistence.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataOrderJpaRepository
        extends JpaRepository<OrderJpaEntity, UUID> {

    Page<OrderJpaEntity> findByCustomerId(UUID customerId, Pageable pageable);
}