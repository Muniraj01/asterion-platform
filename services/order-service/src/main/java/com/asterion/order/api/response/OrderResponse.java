package com.asterion.order.api.response;

import com.asterion.order.domain.model.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        UUID merchantId,
        UUID customerId,
        BigDecimal totalAmount,
        String status,
        Instant createdAt
) {

    public static OrderResponse from(Order order) {

        return new OrderResponse(
                order.id(),
                order.merchantId(),
                order.customerId(),
                order.totalAmount(),
                order.status().name(),
                order.createdAt()
        );
    }
}