
package com.asterion.order.infrastructure.messaging;

import com.asterion.events.PaymentFailedEvent;
import com.asterion.events.PaymentSucceededEvent;
import com.asterion.order.application.port.in.HandlePaymentOutcomeUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PaymentOutcomeKafkaConsumerTest {

    private ObjectMapper objectMapper;
    private HandlePaymentOutcomeUseCase useCase;
    private PaymentOutcomeKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        useCase = mock(HandlePaymentOutcomeUseCase.class);
        consumer = new PaymentOutcomeKafkaConsumer(objectMapper, useCase);
    }

    @Test
    void shouldRouteSuccessfulOutcomeToUseCase() throws Exception {
        PaymentSucceededEvent event = succeededEvent();
        ConsumerRecord<String, String> record = record(
                "payment.succeeded.v1", objectMapper.writeValueAsString(event));

        consumer.consume(record);

        verify(useCase).handle(event);
        verify(useCase, never()).handle(any(PaymentFailedEvent.class));
    }

    @Test
    void shouldRouteFailedOutcomeToUseCase() throws Exception {
        PaymentFailedEvent event = failedEvent();
        ConsumerRecord<String, String> record = record(
                "payment.failed.v1", objectMapper.writeValueAsString(event));

        consumer.consume(record);

        verify(useCase).handle(event);
        verify(useCase, never()).handle(any(PaymentSucceededEvent.class));
    }

    @Test
    void shouldRejectMissingEventTypeHeaderAsPermanentFailure() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "payment.events", 0, 0L, "key", "{}");

        assertThatThrownBy(() -> consumer.consume(record))
                .isInstanceOf(PermanentPaymentOutcomeException.class)
                .hasMessageContaining("Missing Kafka eventType header");

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldRejectUnsupportedEventTypeAsPermanentFailure() {
        ConsumerRecord<String, String> record = record(
                "payment.refunded.v1", "{}");

        assertThatThrownBy(() -> consumer.consume(record))
                .isInstanceOf(PermanentPaymentOutcomeException.class)
                .hasMessageContaining("Unsupported payment outcome event type");

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldRejectMalformedPayloadAsPermanentFailure() {
        ConsumerRecord<String, String> record = record(
                "payment.succeeded.v1", "{invalid-json");

        assertThatThrownBy(() -> consumer.consume(record))
                .isInstanceOf(PermanentPaymentOutcomeException.class)
                .hasMessageContaining("Malformed payment outcome payload");

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldPropagateTransientProcessingFailureWithoutWrapping() {
        RuntimeException transientFailure =
                new RuntimeException("Temporary database failure");

        doThrow(transientFailure)
                .when(useCase).handle(any(PaymentSucceededEvent.class));

        ConsumerRecord<String, String> record = record(
                "payment.succeeded.v1",
                json(succeededEvent()));

        assertThatThrownBy(() -> consumer.consume(record))
                .isSameAs(transientFailure);
    }

    private ConsumerRecord<String, String> record(
            String eventType,
            String payload) {

        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "payment.events", 0, 0L, "key", payload);

        record.headers().add(new RecordHeader(
                "eventType", eventType.getBytes(StandardCharsets.UTF_8)));

        return record;
    }

    private String json(PaymentSucceededEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception exception) {
            throw new AssertionError("Could not serialize test event", exception);
        }
    }

    private PaymentSucceededEvent succeededEvent() {
        return new PaymentSucceededEvent(
                UUID.randomUUID(),
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.0000"));
    }

    private PaymentFailedEvent failedEvent() {
        return new PaymentFailedEvent(
                UUID.randomUUID(),
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.0000"));
    }
}