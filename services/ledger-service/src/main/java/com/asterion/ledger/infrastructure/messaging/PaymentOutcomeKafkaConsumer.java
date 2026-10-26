
package com.asterion.ledger.infrastructure.messaging;

import com.asterion.events.PaymentFailedEvent;
import com.asterion.events.PaymentSucceededEvent;
import com.asterion.ledger.application.PaymentLedgerPostingService;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    private final PaymentLedgerPostingService postingService;

    public PaymentOutcomeKafkaConsumer(
            ObjectMapper objectMapper,
            PaymentLedgerPostingService postingService) {

        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.postingService = Objects.requireNonNull(postingService);
    }

    @KafkaListener(
            topics = "${asterion.ledger.kafka.payment-outcome-topic:payment.events}",
            groupId = "${asterion.ledger.kafka.consumer-group-id:ledger-service}",
            containerFactory = "ledgerKafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, String> record) {

        if (record == null || record.value() == null || record.value().isBlank())
            throw new PermanentLedgerEventException(
                    "Payment outcome payload must not be blank");

        String eventType = readEventType(record);

        switch (eventType) {
            case PAYMENT_SUCCEEDED -> {
                PaymentSucceededEvent event = readPayload(
                        record.value(), PaymentSucceededEvent.class);
                postingService.postSuccessfulPayment(event);
            }

            case PAYMENT_FAILED -> readPayload(
                    record.value(), PaymentFailedEvent.class);

            default -> throw new PermanentLedgerEventException(
                    "Unsupported payment outcome event type: " + eventType);
        }
    }

    private <T> T readPayload(String payload, Class<T> eventClass) {
        try {
            T event = objectMapper.readValue(payload, eventClass);
            if (event == null)
                throw new PermanentLedgerEventException(
                        "Payment outcome payload must not decode to null");

            return event;
        } catch (JsonProcessingException exception) {
            throw new PermanentLedgerEventException(
                    "Malformed payment outcome payload", exception
            );
        }
    }

    private String readEventType(ConsumerRecord<String, String> record) {

        Header header = record.headers().lastHeader(EVENT_TYPE_HEADER);
        if (header == null || header.value() == null || header.value().length == 0)
            throw new PermanentLedgerEventException("Missing Kafka eventType header");

        String eventType = new String(header.value(), StandardCharsets.UTF_8);
        if (eventType.isBlank())
            throw new PermanentLedgerEventException(
                    "Kafka eventType header must not be blank");

        return eventType;
    }
}
