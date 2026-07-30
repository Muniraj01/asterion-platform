package com.asterion.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID customerId,
        BigDecimal totalAmount
) implements DomainEvent {

    @Override
    public String eventType() {
        return "order.created.v1";
    }
}