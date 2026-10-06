package com.asterion.order.application.exception;

import com.asterion.order.domain.model.OrderStatus;

import java.util.UUID;

public class OrderTransitionConflictException extends RuntimeException {

    public OrderTransitionConflictException(
            UUID orderId,
            OrderStatus targetStatus) {

        super("Order transition conflict for order "
                + orderId + " while transitioning to " + targetStatus);
    }
}