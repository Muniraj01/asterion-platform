package com.asterion.order.application.service;

import com.asterion.order.application.command.TransitionOrderCommand;
import com.asterion.order.application.exception.OrderTransitionConflictException;
import com.asterion.order.application.port.in.TransitionOrderUseCase;
import com.asterion.order.application.port.out.OrderRepository;
import com.asterion.order.application.exception.OrderOwnershipException;
import com.asterion.order.domain.model.Order;
import com.asterion.order.domain.model.OrderStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public class TransitionOrderService implements TransitionOrderUseCase {

    private final OrderRepository orderRepository;

    public TransitionOrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional
    public Order transition(TransitionOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        if (command.orderId() == null)
            throw new NullPointerException("orderId must not be null");

        if (command.authenticatedUserId() == null)
            throw new NullPointerException("authenticatedUserId must not be null");

        if (command.targetStatus() == null)
            throw new NullPointerException("targetStatus must not be null");

        if (command.targetStatus() != OrderStatus.CANCELLED
                && command.targetStatus() != OrderStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "Unsupported order transition target: " + command.targetStatus()
            );
        }

        Order order = orderRepository
                .findById(command.orderId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order not found: " + command.orderId())
                );

        if (!order.customerId().equals(command.authenticatedUserId())) {
            throw new OrderOwnershipException(
                    "Authenticated user does not own order: " + command.orderId());
        }

        switch (command.targetStatus()) {
            case CANCELLED -> order.cancel();
            case COMPLETED -> order.complete();
            default -> throw new IllegalArgumentException(
                    "Unsupported order transition target: " + command.targetStatus());
        }

        boolean transitioned = orderRepository.transitionStatus(
                order.id(), OrderStatus.CREATED, command.targetStatus()
        );

        if (!transitioned)
            throw new OrderTransitionConflictException(order.id(), command.targetStatus());

        return order;
    }
}