package com.asterion.merchant.infrastructure.persistence;

import com.asterion.merchant.application.model.MerchantOutboxEvent;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;
import org.springframework.stereotype.Component;

@Component
public class MerchantOutboxRepositoryAdapter implements MerchantOutboxRepository {

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

        return new MerchantOutboxEvent(
                saved.getEventId(),
                saved.getAggregateId(),
                saved.getEventType(),
                saved.getPayload(),
                saved.getCreatedAt(),
                saved.getStatus()
        );
    }
}