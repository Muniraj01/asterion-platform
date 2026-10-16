package com.asterion.order.infrastructure.messaging;

import com.asterion.events.PaymentFailedEvent;
import com.asterion.events.PaymentSucceededEvent;
import com.asterion.order.application.port.in.HandlePaymentOutcomeUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
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
    void shouldRejectMissingEventTypeHeader() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "payment.events", 0, 0L, "key", "{}");

        assertThatThrownBy(() -> consumer.consume(record))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to process payment outcome event");

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldRejectUnsupportedEventType() {
        ConsumerRecord<String, String> record = record(
                "payment.refunded.v1", "{}");

        assertThatThrownBy(() -> consumer.consume(record))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to process payment outcome event");

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldRejectMalformedPayload() {
        ConsumerRecord<String, String> record = record(
                "payment.succeeded.v1", "{invalid-json");

        assertThatThrownBy(() -> consumer.consume(record))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to process payment outcome event");

        verifyNoInteractions(useCase);
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