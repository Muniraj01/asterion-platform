package com.asterion.merchant.application.model;

import java.time.Instant;
import java.util.UUID;

public record MerchantOutboxEvent(
        UUID eventId,
        UUID aggregateId,
        String eventType,
        String payload,
        Instant createdAt,
        String status
) {
}