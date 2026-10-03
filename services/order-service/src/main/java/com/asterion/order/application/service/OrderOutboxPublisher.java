package com.asterion.order.application.service;

import com.asterion.order.application.model.OrderOutboxEvent;
import com.asterion.order.application.port.out.EventPublisher;
import com.asterion.order.application.port.out.OrderOutboxRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class OrderOutboxPublisher {

    private final OrderOutboxRepository repository;
    private final EventPublisher eventPublisher;
    private final Clock clock;
    private final Duration claimLease;

    public OrderOutboxPublisher(
            OrderOutboxRepository repository,
            EventPublisher eventPublisher,
            Clock clock,
            Duration claimLease) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.claimLease = claimLease;
    }

    public void publishPending(int batchSize) {
        Instant now = clock.instant();
        Instant staleBefore = now.minus(claimLease);

        List<OrderOutboxEvent> events =
                repository.claimPending(batchSize, now, staleBefore);

        for (OrderOutboxEvent event : events) {
            try {
                eventPublisher.publish(event);
                repository.markPublished(event.eventId());

            } catch (RuntimeException exception) {
                // Leave the event claim in place. It becomes eligible
                // again after the claim lease expires.
            }
        }
    }
}