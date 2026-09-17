package com.asterion.merchant.application.service;

import com.asterion.merchant.application.model.MerchantOutboxEvent;
import com.asterion.merchant.application.port.out.EventPublisher;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MerchantOutboxPublisherTest {

    private MerchantOutboxRepository merchantOutboxRepository;
    private EventPublisher eventPublisher;
    private MerchantOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        merchantOutboxRepository = mock(MerchantOutboxRepository.class);
        eventPublisher = mock(EventPublisher.class);
        publisher = new MerchantOutboxPublisher(merchantOutboxRepository, eventPublisher);
    }

    @Test
    void shouldPublishPendingEventAndMarkItPublished() {
        MerchantOutboxEvent event = event();
        when(merchantOutboxRepository.findPending(10))
                .thenReturn(List.of(event));

        publisher.publishPending(10);

        verify(eventPublisher).publish(event);
        verify(merchantOutboxRepository).markPublished(event.eventId());
    }

    @Test
    void shouldNotMarkEventPublishedWhenPublishingFails() {
        MerchantOutboxEvent event = event();
        when(merchantOutboxRepository.findPending(10))
                .thenReturn(List.of(event));

        doThrow(new IllegalStateException("Kafka unavailable"))
                .when(eventPublisher)
                .publish(event);

        assertThrows(IllegalStateException.class,
                () -> publisher.publishPending(10));

        verify(eventPublisher).publish(event);
        verify(merchantOutboxRepository, never())
                .markPublished(event.eventId());
    }

    @Test
    void shouldPublishEventsInRepositoryOrder() {
        MerchantOutboxEvent first = event();
        MerchantOutboxEvent second = event();

        when(merchantOutboxRepository.findPending(10))
                .thenReturn(List.of(first, second));

        publisher.publishPending(10);

        var inOrder = inOrder(eventPublisher, merchantOutboxRepository);

        inOrder.verify(eventPublisher).publish(first);
        inOrder.verify(merchantOutboxRepository)
                .markPublished(first.eventId());

        inOrder.verify(eventPublisher).publish(second);
        inOrder.verify(merchantOutboxRepository)
                .markPublished(second.eventId());
    }

    @Test
    void shouldRejectNonPositiveBatchSize() {
        assertThrows(IllegalArgumentException.class,
                () -> publisher.publishPending(0));

        verifyNoInteractions(merchantOutboxRepository, eventPublisher);
    }

    private MerchantOutboxEvent event() {
        return new MerchantOutboxEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "merchant.created.v1",
                "{}",
                Instant.now(),
                "NEW");
    }
}
