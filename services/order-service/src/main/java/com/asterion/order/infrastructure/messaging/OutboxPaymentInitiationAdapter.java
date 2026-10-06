package com.asterion.order.infrastructure.messaging;

import com.asterion.events.PaymentInitiationRequestedEvent;
import com.asterion.order.application.model.OrderOutboxEvent;
import com.asterion.order.application.model.PaymentInitiation;
import com.asterion.order.application.port.out.OrderOutboxRepository;
import com.asterion.order.application.port.out.PaymentInitiationPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class OutboxPaymentInitiationAdapter implements PaymentInitiationPort {

    private final OrderOutboxRepository orderOutboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxPaymentInitiationAdapter(
            OrderOutboxRepository orderOutboxRepository,
            ObjectMapper objectMapper) {
        this.orderOutboxRepository = orderOutboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void initiate(PaymentInitiation paymentInitiation) {
        PaymentInitiationRequestedEvent event =
                new PaymentInitiationRequestedEvent(
                        UUID.randomUUID(),
                        Instant.now(),
                        paymentInitiation.orderId(),
                        paymentInitiation.merchantId(),
                        paymentInitiation.customerId(),
                        paymentInitiation.totalAmount()
                );

        String payload = serialize(event);

        orderOutboxRepository.save(
                new OrderOutboxEvent(
                        event.eventId(),
                        paymentInitiation.orderId(),
                        event.eventType(),
                        payload,
                        event.occurredAt(),
                        "NEW",
                        null
                )
        );
    }

    private String serialize(PaymentInitiationRequestedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize PaymentInitiationRequestedEvent", exception
            );
        }
    }
}