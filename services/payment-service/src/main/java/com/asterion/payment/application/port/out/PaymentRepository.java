package com.asterion.payment.application.port.out;

import com.asterion.payment.domain.model.Payment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Optional<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findBySourceEventId(UUID sourceEventId);

    boolean createIfAbsent(Payment payment);

    Optional<Payment> claimForProcessing(UUID orderId, Instant now,
                                         Instant leaseUntil, int maxAttempts);

    List<UUID> findDueOrderIds(Instant now, int maxAttempts, int limit);

    int failExpiredProcessingBeyondMaxAttempts(Instant now, int maxAttempts);

    void save(Payment payment);
}