package com.asterion.order.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderCommand(
        UUID merchantId,
        UUID authenticatedCustomerId,
        BigDecimal totalAmount
) {
}