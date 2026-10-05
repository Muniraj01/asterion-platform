package com.asterion.order.application.command;

import java.util.UUID;

public record GetOrderCommand(
        UUID orderId,
        UUID authenticatedUserId
) {
}