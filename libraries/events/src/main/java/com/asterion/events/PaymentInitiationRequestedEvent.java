package com.asterion.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentInitiationRequestedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID merchantId,
        UUID customerId,
        BigDecimal totalAmount) implements DomainEvent {

    @Override
    public String eventType() {
        return "payment.initiation.requested.v1";
    }
}