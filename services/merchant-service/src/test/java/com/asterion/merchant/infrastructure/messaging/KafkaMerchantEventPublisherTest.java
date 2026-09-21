package com.asterion.merchant.infrastructure.messaging;

import com.asterion.merchant.application.model.MerchantOutboxEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KafkaMerchantEventPublisherTest {

    private KafkaTemplate<String, String> kafkaTemplate;
    private KafkaMerchantEventPublisher publisher;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        publisher = new KafkaMerchantEventPublisher(kafkaTemplate, "merchant.events");
    }

    @Test
    void shouldPublishEventWithCorrectTopicKeyPayloadAndHeaders() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        MerchantOutboxEvent event = new MerchantOutboxEvent(
                eventId,
                merchantId,
                "merchant.created.v1",
                "{\"merchantId\":\"" + merchantId + "\"}",
                Instant.now(),
                "NEW",
                null
        );

        CompletableFuture<SendResult<String, String>> future =
                CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(future);

        publisher.publish(event);

        var captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        ProducerRecord<String, String> record = captor.getValue();

        assertThat(record.topic()).isEqualTo("merchant.events");
        assertThat(record.key()).isEqualTo(merchantId.toString());
        assertThat(record.value()).isEqualTo(event.payload());
        assertThat(headerValue(record, "eventId"))
                .isEqualTo(eventId.toString());
        assertThat(headerValue(record, "eventType"))
                .isEqualTo("merchant.created.v1");
        assertThat(headerValue(record, "aggregateId"))
                .isEqualTo(merchantId.toString());
    }

    @Test
    void shouldPropagateFailureWhenKafkaPublicationFails() {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new IllegalStateException("Kafka unavailable"));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        MerchantOutboxEvent event = event();
        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to publish merchant outbox event")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRestoreInterruptedFlagWhenPublicationIsInterrupted() throws Exception {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new InterruptedException("Interrupted"));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        // The CompletableFuture.get() implementation normally wraps
        // exceptional completion in ExecutionException. To test the
        // actual InterruptedException path, use a custom future.
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(new InterruptingFuture<>());

        MerchantOutboxEvent event = event();
        try {
            assertThatThrownBy(() -> publisher.publish(event))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Interrupted while publishing merchant outbox event");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void shouldRejectBlankTopic() {
        assertThatThrownBy(() -> new KafkaMerchantEventPublisher(kafkaTemplate, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("topic must not be blank");
    }

    @Test
    void shouldRejectNullEvent() {
        assertThatThrownBy(() -> publisher.publish(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("event must not be null");
    }

    private MerchantOutboxEvent event() {
        return new MerchantOutboxEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "merchant.created.v1",
                "{}",
                Instant.now(),
                "NEW",
                null);
    }

    private String headerValue(ProducerRecord<String, String> record, String name) {
        var header = record.headers().lastHeader(name);
        assertThat(header)
                .as("Expected Kafka header: " + name)
                .isNotNull();
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private static class InterruptingFuture<T> extends CompletableFuture<T> {

        @Override
        public T get() throws InterruptedException, ExecutionException {
            throw new InterruptedException("Interrupted");
        }
    }
}