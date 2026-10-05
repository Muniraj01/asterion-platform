package com.asterion.order.application.service;

import com.asterion.order.application.command.ListOrdersCommand;
import com.asterion.order.application.model.OrderPage;
import com.asterion.order.application.port.in.ListOrdersUseCase;
import com.asterion.order.application.port.out.OrderRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public class ListOrdersService implements ListOrdersUseCase {

    private final OrderRepository orderRepository;

    public ListOrdersService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderPage list(ListOrdersCommand command) {

        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.authenticatedUserId(),
                "authenticatedUserId must not be null");

        if (command.page() < 0)
            throw new IllegalArgumentException("page must not be negative");

        if (command.size() <= 0)
            throw new IllegalArgumentException("size must be greater than zero");

        return orderRepository
                .findByCustomerId(command.authenticatedUserId(), command.page(), command.size());
    }
}