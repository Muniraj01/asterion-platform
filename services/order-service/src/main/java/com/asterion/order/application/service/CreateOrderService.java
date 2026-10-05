package com.asterion.order.application.service;

import com.asterion.events.OrderCreatedEvent;
import com.asterion.order.application.command.CreateOrderCommand;
import com.asterion.order.application.exception.MerchantNotActiveException;
import com.asterion.order.application.model.MerchantValidationResult;
import com.asterion.order.application.model.OrderOutboxEvent;
import com.asterion.order.application.port.in.CreateOrderUseCase;
import com.asterion.order.application.port.out.MerchantValidationPort;
import com.asterion.order.application.port.out.OrderOutboxRepository;
import com.asterion.order.application.port.out.OrderRepository;
import com.asterion.order.domain.model.Order;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CreateOrderService implements CreateOrderUseCase {

    private final MerchantValidationPort merchantValidationPort;
    private final OrderRepository orderRepository;
    private final OrderOutboxRepository orderOutboxRepository;
    private final ObjectMapper objectMapper;

    public CreateOrderService(
            MerchantValidationPort merchantValidationPort,
            OrderRepository orderRepository,
            OrderOutboxRepository orderOutboxRepository,
            ObjectMapper objectMapper) {

        this.merchantValidationPort = merchantValidationPort;
        this.orderRepository = orderRepository;
        this.orderOutboxRepository = orderOutboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Order create(CreateOrderCommand command) {
        validate(command);
        MerchantValidationResult merchant =
                merchantValidationPort.validate(command.merchantId());

        if (!merchant.active())
            throw new MerchantNotActiveException(command.merchantId(), merchant.status());

        Order order = Order.create(
                command.merchantId(),
                command.authenticatedCustomerId(),
                command.totalAmount()
        );

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                order.id(),
                order.merchantId(),
                order.customerId(),
                order.totalAmount()
        );

        String payload = serialize(event);
        OrderOutboxEvent outboxEvent = new OrderOutboxEvent(
                event.eventId(),
                order.id(),
                event.eventType(),
                payload,
                event.occurredAt(),
                "NEW",
                null
        );

        Order savedOrder = orderRepository.save(order);
        orderOutboxRepository.save(outboxEvent);
        return savedOrder;
    }

    private void validate(CreateOrderCommand command) {
        if (command == null)
            throw new IllegalArgumentException("command must not be null");

        if (command.merchantId() == null)
            throw new IllegalArgumentException("merchantId must not be null");

        if (command.authenticatedCustomerId() == null)
            throw new IllegalArgumentException("customerId must not be null");

        if (command.totalAmount() == null)
            throw new IllegalArgumentException("totalAmount must not be null");
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