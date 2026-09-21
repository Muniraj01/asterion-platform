package com.asterion.merchant.application.service;

import com.asterion.merchant.application.model.MerchantOutboxEvent;
import com.asterion.merchant.application.port.out.EventPublisher;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class MerchantOutboxPublisher {

    private static final Duration DEFAULT_CLAIM_LEASE = Duration.ofSeconds(60);
    private final MerchantOutboxRepository merchantOutboxRepository;
    private final EventPublisher eventPublisher;
    private final Clock clock;
    private final Duration claimLease;

    public MerchantOutboxPublisher(MerchantOutboxRepository merchantOutboxRepository,
                                   EventPublisher eventPublisher) {
        this(merchantOutboxRepository, eventPublisher, Clock.systemUTC(), DEFAULT_CLAIM_LEASE);
    }

    public MerchantOutboxPublisher(MerchantOutboxRepository merchantOutboxRepository,
                                   EventPublisher eventPublisher,
                                   Clock clock,
                                   Duration claimLease) {
        this.merchantOutboxRepository = Objects.requireNonNull(
                merchantOutboxRepository, "merchantOutboxRepository must not be null");

        this.eventPublisher = Objects.requireNonNull(
                eventPublisher, "eventPublisher must not be null");

        this.clock = Objects.requireNonNull(
                clock, "clock must not be null");

        this.claimLease = Objects.requireNonNull(
                claimLease, "claimLease must not be null");

        if (claimLease.isZero() || claimLease.isNegative()) {
            throw new IllegalArgumentException("claimLease must be greater than zero");
        }
    }

    public void publishPending(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be greater than zero");
        }

        Instant now = clock.instant();
        Instant staleBefore = now.minus(claimLease);

        List<MerchantOutboxEvent> claimedEvents = merchantOutboxRepository
                .claimPending(batchSize, now, staleBefore);

        for (MerchantOutboxEvent event : claimedEvents) {
            eventPublisher.publish(event);
            merchantOutboxRepository.markPublished(event.eventId());
        }
    }
}