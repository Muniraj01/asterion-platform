package com.asterion.merchant.infrastructure.messaging;

import com.asterion.merchant.application.model.MerchantOutboxEvent;
import com.asterion.merchant.application.port.out.EventPublisher;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.KafkaException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Component
public class KafkaMerchantEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public KafkaMerchantEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${asterion.merchant.kafka.topic}") String topic) {
        this.kafkaTemplate = Objects.requireNonNull(
                kafkaTemplate, "kafkaTemplate must not be null");
        this.topic = Objects.requireNonNull(
                topic, "topic must not be null");
        if (topic.isBlank()) {
            throw new IllegalArgumentException("topic must not be blank");
        }
    }

    @Override
    public void publish(MerchantOutboxEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        ProducerRecord<String, String> record = new ProducerRecord<>(
                topic,
                event.aggregateId().toString(),
                event.payload()
        );

        addHeader(record, "eventId", event.eventId().toString());
        addHeader(record, "eventType", event.eventType());
        addHeader(record, "aggregateId", event.aggregateId().toString());

        try {
            kafkaTemplate.send(record).get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while publishing merchant outbox event", ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException(
                    "Failed to publish merchant outbox event", ex.getCause());
        } catch (KafkaException ex) {
            throw new IllegalStateException(
                    "Failed to publish merchant outbox event", ex);
        }
    }

    private void addHeader(ProducerRecord<String, String> record,
                           String name,
                           String value) {
        record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
    }
}