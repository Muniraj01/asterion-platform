package com.asterion.order.application.command;

import com.asterion.order.domain.model.OrderStatus;

import java.util.UUID;

public record TransitionOrderCommand(
        UUID orderId,
        UUID authenticatedUserId,
        OrderStatus targetStatus) {
}