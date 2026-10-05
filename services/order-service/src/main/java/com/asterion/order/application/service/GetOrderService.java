package com.asterion.order.application.service;

import com.asterion.order.application.command.GetOrderCommand;
import com.asterion.order.application.exception.OrderOwnershipException;
import com.asterion.order.application.port.in.GetOrderUseCase;
import com.asterion.order.application.port.out.OrderRepository;
import com.asterion.order.domain.model.Order;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public class GetOrderService implements GetOrderUseCase {

    private final OrderRepository orderRepository;

    public GetOrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Order get(GetOrderCommand command) {

        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.orderId(), "orderId must not be null");
        Objects.requireNonNull(command.authenticatedUserId(),
                "authenticatedUserId must not be null");

        Order order = orderRepository
                .findById(command.orderId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order not found: " + command.orderId()));

        if (!order.customerId().equals(command.authenticatedUserId())) {
            throw new OrderOwnershipException(
                    "Authenticated user does not own order: " + command.orderId()
            );
        }

        return order;
    }
}