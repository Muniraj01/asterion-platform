package com.asterion.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
            @Param("updatedAt") Instant updatedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE payments
        SET status = 'PROCESSING',
            attempt_count = attempt_count + 1,
            next_attempt_at = NULL,
            processing_lease_until = :leaseUntil,
            updated_at = :now
        WHERE order_id = :orderId
          AND attempt_count < :maxAttempts
          AND (
                status = 'INITIATED'
                OR (
                    status = 'RETRYABLE'
                    AND next_attempt_at <= :now
                )
                OR (
                    status = 'PROCESSING'
                    AND processing_lease_until <= :now
                )
          )
        """, nativeQuery = true)
    int claimForProcessing(
            @Param("orderId") UUID orderId,
            @Param("now") Instant now,
            @Param("leaseUntil") Instant leaseUntil,
            @Param("maxAttempts") int maxAttempts);

    @Query(value = """
        SELECT order_id
        FROM payments
        WHERE attempt_count < :maxAttempts
          AND (
                status = 'INITIATED'
                OR (
                    status = 'RETRYABLE'
                    AND next_attempt_at <= :now
                )
                OR (
                    status = 'PROCESSING'
                    AND processing_lease_until <= :now
                )
          )
        ORDER BY created_at ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<UUID> findDueOrderIds(
            @Param("now") Instant now,
            @Param("maxAttempts") int maxAttempts,
            @Param("limit") int limit);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE payments
        SET status = 'FAILED',
            processing_lease_until = NULL,
            next_attempt_at = NULL,
            updated_at = :now
        WHERE status = 'PROCESSING'
          AND processing_lease_until <= :now
          AND attempt_count >= :maxAttempts
        """, nativeQuery = true)
    int failExpiredProcessingBeyondMaxAttempts(
            @Param("now") Instant now,
            @Param("maxAttempts") int maxAttempts);
}