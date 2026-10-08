package com.asterion.payment.infrastructure.messaging;

import com.asterion.events.PaymentInitiationRequestedEvent;
import com.asterion.payment.application.model.PaymentInitiation;
import com.asterion.payment.application.port.in.InitiatePaymentUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentInitiationKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final InitiatePaymentUseCase initiatePaymentUseCase;

    public PaymentInitiationKafkaConsumer(
            ObjectMapper objectMapper,
            InitiatePaymentUseCase initiatePaymentUseCase) {
        this.objectMapper = objectMapper;
        this.initiatePaymentUseCase = initiatePaymentUseCase;
    }

    @KafkaListener(
            topics = "${asterion.payment.kafka.topic}",
            groupId = "${asterion.payment.kafka.consumer.group-id}",
            containerFactory = "paymentKafkaListenerContainerFactory")
    public void consume(String payload) {
        try {
            PaymentInitiationRequestedEvent event =
                    objectMapper.readValue(payload, PaymentInitiationRequestedEvent.class);

            initiatePaymentUseCase.initiate(
                    new PaymentInitiation(
                            event.eventId(),
                            event.occurredAt(),
                            event.orderId(),
                            event.merchantId(),
                            event.customerId(),
                            event.totalAmount()
                    )
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to process payment initiation event", exception);
        }
    }
}