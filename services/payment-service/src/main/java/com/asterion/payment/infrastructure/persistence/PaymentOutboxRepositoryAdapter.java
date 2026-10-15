
package com.asterion.payment.infrastructure.persistence;

import com.asterion.payment.application.model.PaymentOutboxEvent;
import com.asterion.payment.application.port.out.PaymentOutboxRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PaymentOutboxRepositoryAdapter implements PaymentOutboxRepository {

    private static final String NEW_STATUS = "NEW";
    private static final String PROCESSING_STATUS = "PROCESSING";

    private final PaymentOutboxJpaRepository repository;
    private final EntityManager entityManager;

    public PaymentOutboxRepositoryAdapter(
            PaymentOutboxJpaRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public List<PaymentOutboxEvent> claimPending(
            int limit, Instant now, Instant staleBefore) {

        if (limit <= 0)
            throw new IllegalArgumentException("limit must be greater than zero");

        Query query = entityManager.createNativeQuery("""
                SELECT event_id
                FROM payment_outbox_events
                WHERE status = :newStatus
                   OR (
                       status = :processingStatus
                       AND claimed_at < :staleBefore
                   )
                ORDER BY created_at ASC
                FOR UPDATE SKIP LOCKED
                LIMIT :limit
                """);

        query.setParameter("newStatus", NEW_STATUS);
        query.setParameter("processingStatus", PROCESSING_STATUS);
        query.setParameter("staleBefore", staleBefore);
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object> rawIds = query.getResultList();

        if (rawIds.isEmpty())
            return List.of();

        List<UUID> ids = rawIds.stream()
                .map(this::toUuid)
                .toList();

        List<PaymentOutboxJpaEntity> entities = ids.stream()
                .map(repository::findById)
                .flatMap(Optional::stream)
                .toList();

        entities.forEach(entity -> entity.markClaimed(now));
        repository.flush();

        return entities.stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    @Transactional
    public void markPublished(UUID eventId) {
        PaymentOutboxJpaEntity entity = repository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment outbox event not found: " + eventId));

        entity.markPublished(Instant.now());
        repository.save(entity);
    }

    private UUID toUuid(Object value) {
        return value instanceof UUID uuid
                ? uuid : UUID.fromString(value.toString());
    }

    private PaymentOutboxEvent toModel(PaymentOutboxJpaEntity entity) {
        return new PaymentOutboxEvent(
                entity.getEventId(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getCreatedAt(),
                entity.getStatus(),
                entity.getClaimedAt());
    }
}
