
package com.asterion.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID paymentId,
        UUID orderId,
        UUID merchantId,
        UUID customerId,
        BigDecimal totalAmount
) implements DomainEvent {

    @Override
    public String eventType() {
        return "payment.failed.v1";
    }
}
