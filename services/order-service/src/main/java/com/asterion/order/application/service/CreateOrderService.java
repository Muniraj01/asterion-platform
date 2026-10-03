package com.asterion.order.application.service;

import com.asterion.events.OrderCreatedEvent;
import com.asterion.order.application.command.CreateOrderCommand;
import com.asterion.order.application.model.OrderOutboxEvent;
import com.asterion.order.application.port.in.CreateOrderUseCase;
import com.asterion.order.application.port.out.OrderOutboxRepository;
import com.asterion.order.application.port.out.OrderRepository;
import com.asterion.order.domain.model.Order;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class CreateOrderService implements CreateOrderUseCase {

    private static final String NEW_STATUS = "NEW";
    private final OrderRepository orderRepository;
    private final OrderOutboxRepository orderOutboxRepository;
    private final ObjectMapper objectMapper;

    public CreateOrderService(
            OrderRepository orderRepository,
            OrderOutboxRepository orderOutboxRepository,
            ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.orderOutboxRepository = orderOutboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Order create(CreateOrderCommand command) {
        requireNonNull(command, "command must not be null");
        requireNonNull(command.merchantId(), "merchantId must not be null");
        requireNonNull(command.authenticatedCustomerId(),
                "authenticatedCustomerId must not be null");

        if (command.totalAmount() == null)
            throw new IllegalArgumentException("totalAmount must not be null");

        Order order = Order.create(
                command.merchantId(),
                command.authenticatedCustomerId(),
                command.totalAmount()
        );

        Order savedOrder = orderRepository.save(order);

        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId,
                occurredAt,
                savedOrder.id(),
                savedOrder.merchantId(),
                savedOrder.customerId(),
                savedOrder.totalAmount()
        );

        String payload = serialize(event);
        OrderOutboxEvent outboxEvent = new OrderOutboxEvent(
                eventId,
                savedOrder.id(),
                event.eventType(),
                payload,
                occurredAt,
                NEW_STATUS,
                null
        );

        orderOutboxRepository.save(outboxEvent);
        return savedOrder;
    }

    private String serialize(OrderCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize OrderCreatedEvent", exception);
        }
    }
}