package com.asterion.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataPaymentJpaRepository
        extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByOrderId(UUID orderId);

    Optional<PaymentJpaEntity> findBySourceEventId(UUID sourceEventId);

    @Modifying
    @Query(value = """
            INSERT INTO payments (
                payment_id,
                order_id,
                merchant_id,
                customer_id,
                source_event_id,
                total_amount,
                status,
                created_at,
                updated_at
            )
            VALUES (
                :paymentId,
                :orderId,
                :merchantId,
                :customerId,
                :sourceEventId,
                :totalAmount,
                :status,
                :createdAt,
                :updatedAt
            )
            ON CONFLICT (order_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("paymentId") UUID paymentId,
            @Param("orderId") UUID orderId,
            @Param("merchantId") UUID merchantId,
            @Param("customerId") UUID customerId,
            @Param("sourceEventId") UUID sourceEventId,
            @Param("totalAmount") BigDecimal totalAmount,
            @Param("status") String status,
            @Param("createdAt") Instant createdAt,
            @Param("updatedAt") Instant updatedAt
    );
}