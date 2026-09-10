package com.asterion.merchant.application.event;

import java.time.Instant;
import java.util.UUID;

public record MerchantCreatedEvent(
        UUID eventId,
        UUID merchantId,
        UUID ownerUserId,
        String businessName,
        String legalName,
        String contactEmail,
        Instant occurredAt
) {
}