package com.asterion.order.infrastructure.persistence;

import com.asterion.order.application.model.OrderOutboxEvent;
import com.asterion.order.application.port.out.OrderOutboxRepository;
import com.asterion.order.infrastructure.persistence.jpa.OrderOutboxJpaEntity;
import com.asterion.order.infrastructure.persistence.jpa.SpringDataOrderOutboxJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class OrderOutboxRepositoryAdapter implements OrderOutboxRepository {

    private final SpringDataOrderOutboxJpaRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public OrderOutboxRepositoryAdapter(
            SpringDataOrderOutboxJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public OrderOutboxEvent save(OrderOutboxEvent event) {
        OrderOutboxJpaEntity entity = new OrderOutboxJpaEntity(
                event.eventId(),
                event.aggregateId(),
                event.eventType(),
                event.payload(),
                event.createdAt(),
                event.status(),
                event.claimedAt()
        );

        repository.save(entity);
        return event;
    }

    @Override
    public List<OrderOutboxEvent> findPending(int limit) {
        return repository
                .findByStatusOrderByCreatedAtAsc("NEW")
                .stream()
                .limit(limit)
                .map(this::toModel)
                .toList();
    }

    @Override
    @Transactional
    public List<OrderOutboxEvent> claimPending(int limit, Instant now, Instant staleBefore) {
        @SuppressWarnings("unchecked")
        List<OrderOutboxJpaEntity> entities =
                entityManager.createNativeQuery(
                                """
                                SELECT *
                                FROM order_outbox_events
                                WHERE status = 'NEW'
                                   OR (status = 'CLAIMED' AND claimed_at < :staleBefore)
                                ORDER BY created_at
                                FOR UPDATE SKIP LOCKED
                                LIMIT :limit
                                """,
                                OrderOutboxJpaEntity.class)
                        .setParameter("staleBefore", staleBefore)
                        .setParameter("limit", limit)
                        .getResultList();

        entities.forEach(entity -> entity.markClaimed(now));

        return entities.stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    @Transactional
    public void markPublished(UUID eventId) {
        OrderOutboxJpaEntity entity = repository
                .findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Outbox event not found: " + eventId));

        entity.markPublished(Instant.now());
    }

    private OrderOutboxEvent toModel(OrderOutboxJpaEntity entity) {
        return new OrderOutboxEvent(
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