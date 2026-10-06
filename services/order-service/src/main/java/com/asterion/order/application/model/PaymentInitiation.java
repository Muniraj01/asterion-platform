package com.asterion.order.application.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record PaymentInitiation(UUID orderId,
                                UUID merchantId,
                                UUID customerId,
                                BigDecimal totalAmount) {

    public PaymentInitiation {
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(merchantId, "merchantId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(totalAmount, "totalAmount must not be null");

        if (totalAmount.signum() < 0)
            throw new IllegalArgumentException("totalAmount must not be negative");
    }
}