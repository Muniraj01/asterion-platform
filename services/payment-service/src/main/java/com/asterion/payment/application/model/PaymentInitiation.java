package com.asterion.payment.application.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PaymentInitiation(
        UUID eventId,
        Instant occurredAt,
        UUID orderId,
        UUID merchantId,
        UUID customerId,
        BigDecimal totalAmount
) {

    public PaymentInitiation {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(orderId);
        Objects.requireNonNull(merchantId);
        Objects.requireNonNull(customerId);
        Objects.requireNonNull(totalAmount);

        if (totalAmount.signum() < 0)
            throw new IllegalArgumentException("Payment amount must not be negative");
    }
}