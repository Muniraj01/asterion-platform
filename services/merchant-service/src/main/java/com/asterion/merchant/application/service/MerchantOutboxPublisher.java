package com.asterion.merchant.application.service;

import com.asterion.merchant.application.model.MerchantOutboxEvent;
import com.asterion.merchant.application.port.out.EventPublisher;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;

import java.util.List;
import java.util.Objects;

public class MerchantOutboxPublisher {

    private final MerchantOutboxRepository merchantOutboxRepository;
    private final EventPublisher eventPublisher;

    public MerchantOutboxPublisher(MerchantOutboxRepository merchantOutboxRepository,
                                   EventPublisher eventPublisher) {
        this.merchantOutboxRepository = Objects.requireNonNull(
                merchantOutboxRepository, "merchantOutboxRepository must not be null");
        this.eventPublisher = Objects.requireNonNull(
                eventPublisher, "eventPublisher must not be null");
    }

    public void publishPending(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be greater than zero");
        }
        List<MerchantOutboxEvent> pendingEvents = merchantOutboxRepository.findPending(batchSize);
        for (MerchantOutboxEvent event : pendingEvents) {
            eventPublisher.publish(event);
            merchantOutboxRepository.markPublished(event.eventId());
        }
    }
}
