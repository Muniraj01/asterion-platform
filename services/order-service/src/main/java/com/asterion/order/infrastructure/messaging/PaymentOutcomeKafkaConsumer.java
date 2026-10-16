package com.asterion.order.infrastructure.messaging;

import com.asterion.events.PaymentFailedEvent;
import com.asterion.events.PaymentSucceededEvent;
import com.asterion.order.application.port.in.HandlePaymentOutcomeUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public class PaymentOutcomeKafkaConsumer {

    private static final String EVENT_TYPE_HEADER = "eventType";
    private static final String PAYMENT_SUCCEEDED = "payment.succeeded.v1";
    private static final String PAYMENT_FAILED = "payment.failed.v1";

    private final ObjectMapper objectMapper;
    private final HandlePaymentOutcomeUseCase handlePaymentOutcomeUseCase;

    public PaymentOutcomeKafkaConsumer(
            ObjectMapper objectMapper,
            HandlePaymentOutcomeUseCase handlePaymentOutcomeUseCase) {

        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.handlePaymentOutcomeUseCase =
                Objects.requireNonNull(handlePaymentOutcomeUseCase);
    }

    @KafkaListener(
            topics = "${asterion.order.payment-outcome.kafka.topic:payment.events}",
            groupId = "${asterion.order.payment-outcome.kafka.consumer.group-id:order-service}",
            containerFactory = "orderPaymentOutcomeKafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            if (record == null || record.value() == null || record.value().isBlank())
                throw new IllegalArgumentException(
                        "Payment outcome payload must not be blank");

            String eventType = readEventType(record);

            switch (eventType) {
                case PAYMENT_SUCCEEDED -> {
                    PaymentSucceededEvent event = objectMapper
                            .readValue(record.value(), PaymentSucceededEvent.class);
                    handlePaymentOutcomeUseCase.handle(event);
                }
                case PAYMENT_FAILED -> {
                    PaymentFailedEvent event = objectMapper
                            .readValue(record.value(), PaymentFailedEvent.class);
                    handlePaymentOutcomeUseCase.handle(event);
                }
                default -> throw new IllegalArgumentException(
                        "Unsupported payment outcome event type: " + eventType);
            }
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to process payment outcome event", exception);
        }
    }

    private String readEventType(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader(EVENT_TYPE_HEADER);

        if (header == null || header.value() == null || header.value().length == 0)
            throw new IllegalArgumentException("Missing Kafka eventType header");

        String eventType = new String(header.value(), StandardCharsets.UTF_8);
        if (eventType.isBlank())
            throw new IllegalArgumentException("Kafka eventType header must not be blank");

        return eventType;
    }
}