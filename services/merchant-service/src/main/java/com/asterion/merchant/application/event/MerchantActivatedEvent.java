package com.asterion.merchant.application.event;

import java.time.Instant;
import java.util.UUID;

public record MerchantActivatedEvent(
        UUID eventId,
        UUID merchantId,
        UUID ownerUserId,
        Instant occurredAt
) {
}