package com.asterion.order.infrastructure.persistence.jpa;

import com.asterion.order.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SpringDataOrderJpaRepository
        extends JpaRepository<OrderJpaEntity, UUID> {

    Page<OrderJpaEntity> findByCustomerId(UUID customerId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OrderJpaEntity o
               set o.status = :targetStatus
             where o.orderId = :orderId
               and o.status = :expectedStatus
            """)
    int transitionStatus(@Param("orderId") UUID orderId,
                         @Param("expectedStatus") OrderStatus expectedStatus,
                         @Param("targetStatus") OrderStatus targetStatus);
}