
package com.asterion.payment.application.service;

import com.asterion.payment.application.model.PaymentOutboxEvent;
import com.asterion.payment.application.port.out.PaymentOutcomeEventPublisher;
import com.asterion.payment.application.port.out.PaymentOutboxRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxPublisherTest {

    @Mock
    private PaymentOutboxRepository repository;

    @Mock
    private PaymentOutcomeEventPublisher eventPublisher;

    private Instant now;
    private PaymentOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        now = Instant.parse("2026-10-14T10:00:00Z");
        publisher = new PaymentOutboxPublisher(
                repository,
                eventPublisher,
                Clock.fixed(now, ZoneOffset.UTC),
                Duration.ofSeconds(60));
    }

    private PaymentOutboxEvent event() {
        return new PaymentOutboxEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "payment.succeeded.v1",
                "{\"eventType\":\"payment.succeeded.v1\"}",
                now,
                "PROCESSING",
                now);
    }

    @Test
    void shouldPublishClaimedEventsAndMarkThemPublished() {
        PaymentOutboxEvent event = event();

        when(repository
                .claimPending(100, now, now.minusSeconds(60)))
                .thenReturn(List.of(event));

        publisher.publishPending(100);

        verify(eventPublisher).publish(event);
        verify(repository).markPublished(event.eventId());
    }

    @Test
    void shouldLeaveEventForRetryWhenPublishingFails() {
        PaymentOutboxEvent event = event();

        when(repository
                .claimPending(100, now, now.minusSeconds(60)))
                .thenReturn(List.of(event));

        doThrow(new IllegalStateException("Kafka unavailable"))
                .when(eventPublisher).publish(event);

        publisher.publishPending(100);
        verify(repository, never()).markPublished(event.eventId());
    }

    @Test
    void shouldRejectInvalidBatchSize() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> publisher.publishPending(0));

        verifyNoInteractions(repository, eventPublisher);
    }
}
