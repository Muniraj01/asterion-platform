
package com.asterion.payment.application.service;

import com.asterion.payment.application.model.PaymentOutboxEvent;
import com.asterion.payment.application.port.out.PaymentOutcomeEventPublisher;
import com.asterion.payment.application.port.out.PaymentOutboxRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class PaymentOutboxPublisher {

    private final PaymentOutboxRepository repository;
    private final PaymentOutcomeEventPublisher eventPublisher;
    private final Clock clock;
    private final Duration claimLease;

    public PaymentOutboxPublisher(
            PaymentOutboxRepository repository,
            PaymentOutcomeEventPublisher eventPublisher,
            Clock clock,
            Duration claimLease) {

        if (claimLease == null || claimLease.isNegative() || claimLease.isZero())
            throw new IllegalArgumentException("Claim lease duration must be positive");

        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.claimLease = claimLease;
    }

    public void publishPending(int batchSize) {
        if (batchSize <= 0)
            throw new IllegalArgumentException("Batch size must be greater than zero");

        Instant now = clock.instant();
        List<PaymentOutboxEvent> events = repository.claimPending(
                batchSize, now, now.minus(claimLease));

        for (PaymentOutboxEvent event : events) {
            try {
                eventPublisher.publish(event);
                repository.markPublished(event.eventId());
            } catch (RuntimeException exception) {
                // The claim expires and the event becomes eligible again.
                // A successful Kafka send followed by a failed DB update
                // can cause redelivery; consumers deduplicate by eventId.
            }
        }
    }
}
