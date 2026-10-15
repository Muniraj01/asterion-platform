
package com.asterion.payment.infrastructure.messaging;

import com.asterion.payment.application.model.PaymentOutboxEvent;
import com.asterion.payment.application.port.out.PaymentOutcomeEventPublisher;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Component
public class KafkaPaymentOutcomeEventPublisher
        implements PaymentOutcomeEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public KafkaPaymentOutcomeEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${asterion.payment.outcome.kafka.topic:payment.events}")
            String topic) {

        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate);
        this.topic = Objects.requireNonNull(topic);

        if (topic.isBlank())
            throw new IllegalArgumentException(
                    "Payment outcome Kafka topic must not be blank");
    }

    @Override
    public void publish(PaymentOutboxEvent event) {
        ProducerRecord<String, String> record = new ProducerRecord<>(
                topic, event.aggregateId().toString(), event.payload());

        addHeader(record, "eventId", event.eventId().toString());
        addHeader(record, "eventType", event.eventType());
        addHeader(record, "aggregateId", event.aggregateId().toString());

        try {
            kafkaTemplate.send(record).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while publishing payment outcome event", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException(
                    "Failed to publish payment outcome event", exception.getCause());
        } catch (KafkaException exception) {
            throw new IllegalStateException(
                    "Failed to publish payment outcome event", exception);
        }
    }

    private void addHeader(
            ProducerRecord<String, String> record, String name, String value) {
        record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
    }
}
