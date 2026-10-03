package com.asterion.order.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataOrderOutboxJpaRepository
        extends JpaRepository<OrderOutboxJpaEntity, UUID> {

    List<OrderOutboxJpaEntity> findByStatusOrderByCreatedAtAsc(String status);
}