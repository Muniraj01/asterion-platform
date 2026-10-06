package com.asterion.order.application.exception;

import com.asterion.order.domain.model.OrderStatus;

public class InvalidOrderStateTransitionException extends RuntimeException {

    public InvalidOrderStateTransitionException(
            OrderStatus currentStatus,
            OrderStatus targetStatus) {

        super("Invalid order state transition: "
                + currentStatus + " -> " + targetStatus);
    }
}