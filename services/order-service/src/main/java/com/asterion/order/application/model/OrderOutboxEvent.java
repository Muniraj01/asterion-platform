package com.asterion.order.application.model;

import java.time.Instant;
import java.util.UUID;

public record OrderOutboxEvent(
        UUID eventId,
        UUID aggregateId,
        String eventType,
        String payload,
        Instant createdAt,
        String status,
        Instant claimedAt
) {
}