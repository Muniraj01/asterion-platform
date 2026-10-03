package com.asterion.order.infrastructure.messaging;

import com.asterion.order.application.model.OrderOutboxEvent;
import com.asterion.order.application.port.out.EventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaOrderEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public KafkaOrderEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${asterion.order.kafka.topic}")
            String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(OrderOutboxEvent event) {
        try {
            kafkaTemplate
                    .send(topic, event.aggregateId().toString(), event.payload())
                    .get();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to publish order event: " + event.eventId(), exception
            );
        }
    }
}