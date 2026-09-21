package com.asterion.merchant.application.service;

import com.asterion.merchant.application.model.MerchantOutboxEvent;
import com.asterion.merchant.application.port.out.EventPublisher;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MerchantOutboxPublisherTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant STALE_BEFORE = Instant.parse("2026-01-01T09:59:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration CLAIM_LEASE = Duration.ofMinutes(1);

    private MerchantOutboxRepository merchantOutboxRepository;
    private EventPublisher eventPublisher;
    private MerchantOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        merchantOutboxRepository = mock(MerchantOutboxRepository.class);
        eventPublisher = mock(EventPublisher.class);
        publisher = new MerchantOutboxPublisher(
                merchantOutboxRepository,
                eventPublisher,
                FIXED_CLOCK,
                CLAIM_LEASE
        );
    }

    @Test
    void shouldClaimEventPublishItAndMarkItPublished() {
        MerchantOutboxEvent event = event();
        when(merchantOutboxRepository
                .claimPending(10, NOW, STALE_BEFORE))
                .thenReturn(List.of(event));

        publisher.publishPending(10);

        verify(merchantOutboxRepository).claimPending(10, NOW, STALE_BEFORE);
        verify(eventPublisher).publish(event);
        verify(merchantOutboxRepository).markPublished(event.eventId());
    }

    @Test
    void shouldNotMarkEventPublishedWhenPublishingFails() {
        MerchantOutboxEvent event = event();
        when(merchantOutboxRepository
                .claimPending(10, NOW, STALE_BEFORE))
                .thenReturn(List.of(event));

        doThrow(new IllegalStateException("Kafka unavailable"))
                .when(eventPublisher)
                .publish(event);

        assertThrows(IllegalStateException.class,
                () -> publisher.publishPending(10));

        verify(merchantOutboxRepository).claimPending(10, NOW, STALE_BEFORE);
        verify(eventPublisher).publish(event);
        verify(merchantOutboxRepository, never()).markPublished(event.eventId());
    }

    @Test
    void shouldPublishClaimedEventsInRepositoryOrder() {
        MerchantOutboxEvent first = event();
        MerchantOutboxEvent second = event();
        when(merchantOutboxRepository
                .claimPending(10, NOW, STALE_BEFORE))
                .thenReturn(List.of(first, second));

        publisher.publishPending(10);
        var inOrder = inOrder(merchantOutboxRepository, eventPublisher);

        inOrder.verify(merchantOutboxRepository).claimPending(10, NOW, STALE_BEFORE);
        inOrder.verify(eventPublisher).publish(first);
        inOrder.verify(merchantOutboxRepository).markPublished(first.eventId());
        inOrder.verify(eventPublisher).publish(second);
        inOrder.verify(merchantOutboxRepository).markPublished(second.eventId());
    }

    @Test
    void shouldRejectNonPositiveBatchSize() {
        assertThrows(IllegalArgumentException.class,
                () -> publisher.publishPending(0));
        verifyNoInteractions(merchantOutboxRepository, eventPublisher);
    }

    @Test
    void shouldRejectNonPositiveClaimLease() {
        assertThrows(IllegalArgumentException.class,
                () -> new MerchantOutboxPublisher(
                        merchantOutboxRepository,
                        eventPublisher,
                        FIXED_CLOCK,
                        Duration.ZERO
                )
        );
    }

    private MerchantOutboxEvent event() {
        return new MerchantOutboxEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "merchant.created.v1",
                "{}",
                NOW,
                "PROCESSING",
                NOW
        );
    }
}