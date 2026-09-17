package com.asterion.merchant.infrastructure.persistence;

import com.asterion.merchant.application.model.MerchantOutboxEvent;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class MerchantOutboxRepositoryAdapter implements MerchantOutboxRepository {

    private static final String NEW_STATUS = "NEW";

    private final MerchantOutboxJpaRepository repository;

    public MerchantOutboxRepositoryAdapter(MerchantOutboxJpaRepository repository) {
        this.repository = repository;
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
                event.status()
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
                .findByStatusOrderByCreatedAtAsc(
                        NEW_STATUS, PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void markPublished(UUID eventId) {
        MerchantOutboxJpaEntity entity = repository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Outbox event not found: " + eventId));
        entity.markPublished(Instant.now());
        repository.save(entity);
    }

    private MerchantOutboxEvent toDomain(MerchantOutboxJpaEntity entity) {
        return new MerchantOutboxEvent(
                entity.getEventId(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getCreatedAt(),
                entity.getStatus()
        );
    }
}
