package com.asterion.merchant.infrastructure.persistence;

import com.asterion.merchant.application.model.MerchantOutboxEvent;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MerchantOutboxRepositoryAdapter implements MerchantOutboxRepository {

    private static final String NEW_STATUS = "NEW";
    private static final String PROCESSING_STATUS = "PROCESSING";

    private final MerchantOutboxJpaRepository repository;
    private final EntityManager entityManager;

    public MerchantOutboxRepositoryAdapter(MerchantOutboxJpaRepository repository,
                                           EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public MerchantOutboxEvent save(MerchantOutboxEvent event) {
        MerchantOutboxJpaEntity entity = new MerchantOutboxJpaEntity(
                event.eventId(),
                event.aggregateId(),
                event.eventType(),
                event.payload(),
                event.createdAt(),
                null,
                event.status(),
                event.claimedAt()
        );
        MerchantOutboxJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<MerchantOutboxEvent> findPending(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than zero");
        }
        return repository
                .findByStatusOrderByCreatedAtAsc(NEW_STATUS,
                        PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public List<MerchantOutboxEvent> claimPending(int limit,
                                                  Instant now,
                                                  Instant staleBefore) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than zero");
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        if (staleBefore == null) {
            throw new IllegalArgumentException("staleBefore must not be null");
        }

        Query query = entityManager.createNativeQuery("""
                SELECT event_id
                FROM merchant_outbox
                WHERE
                    status = :newStatus
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
        List<Object> rawEventIds = query.getResultList();

        if (rawEventIds.isEmpty())
            return List.of();

        List<UUID> eventIds = rawEventIds
                .stream()
                .map(this::toUuid)
                .toList();

        List<MerchantOutboxJpaEntity> entities = eventIds
                .stream()
                .map(repository::findById)
                .flatMap(Optional::stream)
                .toList();

        for (MerchantOutboxJpaEntity entity : entities) {
            entity.markClaimed(now);
        }

        repository.flush();

        return entities
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void markPublished(UUID eventId) {
        MerchantOutboxJpaEntity entity = repository
                .findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Outbox event not found: " + eventId));

        entity.markPublished(Instant.now());
        repository.save(entity);
    }

    private UUID toUuid(Object value) {
        if (value instanceof UUID uuid)
            return uuid;
        return UUID.fromString(value.toString());
    }

    private MerchantOutboxEvent toDomain(MerchantOutboxJpaEntity entity) {
        return new MerchantOutboxEvent(
                entity.getEventId(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getCreatedAt(),
                entity.getStatus(),
                entity.getClaimedAt()
        );
    }
}