package com.asterion.events;

import java.time.Instant;
import java.util.UUID;

public sealed interface DomainEvent
        permits OrderCreatedEvent {

    UUID eventId();

    Instant occurredAt();

    /**
     * Stable, versioned event type identifier.
     * Example: order.created.v1
     */
    String eventType();
}