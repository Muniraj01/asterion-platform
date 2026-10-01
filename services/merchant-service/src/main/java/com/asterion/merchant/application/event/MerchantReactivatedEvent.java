package com.asterion.merchant.application.event;

import java.time.Instant;
import java.util.UUID;

public record MerchantReactivatedEvent(
        UUID eventId,
        UUID merchantId,
        UUID ownerUserId,
        Instant occurredAt
) {
}