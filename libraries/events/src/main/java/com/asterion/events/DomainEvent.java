
package com.asterion.events;

import java.time.Instant;
import java.util.UUID;

public sealed interface DomainEvent
        permits OrderCreatedEvent,
        PaymentInitiationRequestedEvent,
        PaymentSucceededEvent,
        PaymentFailedEvent {

    UUID eventId();

    Instant occurredAt();

    /**
     * Stable, versioned event type identifier.
     */
    String eventType();
}
