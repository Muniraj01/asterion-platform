
package com.asterion.payment.infrastructure.persistence;

import com.asterion.events.DomainEvent;
import com.asterion.events.PaymentFailedEvent;
import com.asterion.events.PaymentSucceededEvent;
import com.asterion.payment.application.port.out.PaymentRepository;
import com.asterion.payment.domain.model.Payment;
import com.asterion.payment.domain.model.PaymentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final SpringDataPaymentJpaRepository repository;
    private final PaymentOutboxJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public PaymentRepositoryAdapter(
            SpringDataPaymentJpaRepository repository,
            PaymentOutboxJpaRepository outboxRepository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId)
                .map(PaymentJpaEntity::toDomain);
    }

    @Override
    public Optional<Payment> findBySourceEventId(UUID sourceEventId) {
        return repository.findBySourceEventId(sourceEventId)
                .map(PaymentJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public boolean createIfAbsent(Payment payment) {
        return repository.insertIfAbsent(
                payment.paymentId(),
                payment.orderId(),
                payment.merchantId(),
                payment.customerId(),
                payment.sourceEventId(),
                payment.totalAmount(),
                payment.status().name(),
                payment.createdAt(),
                payment.updatedAt()
        ) == 1;
    }

    @Override
    @Transactional
    public Optional<Payment> claimForProcessing(
            UUID orderId, Instant now, Instant leaseUntil, int maxAttempts) {

        int claimed = repository.claimForProcessing(
                orderId, now, leaseUntil, maxAttempts);

        if (claimed != 1)
            return Optional.empty();

        return repository.findByOrderId(orderId)
                .map(PaymentJpaEntity::toDomain);
    }

    @Override
    public List<UUID> findDueOrderIds(
            Instant now, int maxAttempts, int limit) {
        return repository.findDueOrderIds(now, maxAttempts, limit);
    }

    /**
     * Fails expired, exhausted processing attempts and writes each failure
     * event in the same transaction. Row locks prevent competing instances
     * from independently processing the same payment.
     */
    @Override
    @Transactional
    public int failExpiredProcessingBeyondMaxAttempts(
            Instant now, int maxAttempts) {

        List<PaymentJpaEntity> expired =
                repository.findExpiredProcessingBeyondMaxAttempts(now, maxAttempts);

        for (PaymentJpaEntity entity : expired) {
            Payment payment = entity.toDomain();
            payment.fail(now);
            entity.updateFromDomain(payment);

            saveOutboxEvent(new PaymentFailedEvent(
                    UUID.randomUUID(),
                    now,
                    payment.paymentId(),
                    payment.orderId(),
                    payment.merchantId(),
                    payment.customerId(),
                    payment.totalAmount()
            ));
        }

        repository.flush();
        return expired.size();
    }

    @Override
    @Transactional
    public void save(Payment payment) {
        PaymentJpaEntity entity = repository.findById(payment.paymentId())
                .orElseThrow(() -> new IllegalStateException(
                        "Payment not found: " + payment.paymentId()));

        entity.updateFromDomain(payment);
        repository.saveAndFlush(entity);
    }

    @Override
    @Transactional
    public void saveWithOutcome(Payment payment, DomainEvent event) {
        if (payment.status() != PaymentStatus.SUCCEEDED
                && payment.status() != PaymentStatus.FAILED) {
            throw new IllegalArgumentException(
                    "Outcome events require a terminal payment status");
        }

        boolean succeeded = event instanceof PaymentSucceededEvent;
        boolean failed = event instanceof PaymentFailedEvent;

        if ((!succeeded && !failed)
                || (succeeded && payment.status() != PaymentStatus.SUCCEEDED)
                || (failed && payment.status() != PaymentStatus.FAILED)) {
            throw new IllegalArgumentException(
                    "Payment status and outcome event type do not match");
        }

        PaymentJpaEntity entity = repository.findById(payment.paymentId())
                .orElseThrow(() -> new IllegalStateException(
                        "Payment not found: " + payment.paymentId()));

        entity.updateFromDomain(payment);
        repository.saveAndFlush(entity);
        saveOutboxEvent(event);
    }

    private void saveOutboxEvent(DomainEvent event) {
        final String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize payment" +
                    " outcome event: " + event.eventId(), exception);
        }

        UUID aggregateId;
        if (event instanceof PaymentSucceededEvent succeeded) {
            aggregateId = succeeded.paymentId();
        } else if (event instanceof PaymentFailedEvent failed) {
            aggregateId = failed.paymentId();
        } else {
            throw new IllegalArgumentException(
                    "Unsupported payment outbox event: " + event.getClass().getName());
        }

        outboxRepository.saveAndFlush(new PaymentOutboxJpaEntity(
                event.eventId(),
                aggregateId,
                event.eventType(),
                payload,
                event.occurredAt()
        ));
    }
}
