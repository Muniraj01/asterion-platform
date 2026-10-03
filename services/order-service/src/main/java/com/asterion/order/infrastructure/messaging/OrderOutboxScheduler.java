package com.asterion.order.infrastructure.messaging;

import com.asterion.order.application.service.OrderOutboxPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderOutboxScheduler {

    private final OrderOutboxPublisher publisher;
    private final int batchSize;

    public OrderOutboxScheduler(OrderOutboxPublisher publisher,
                                @Value("${asterion.order.outbox.batch-size:100}")
                                int batchSize) {
        this.publisher = publisher;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${asterion.order.outbox.poll-interval-ms:1000}")
    public void publishPending() {
        publisher.publishPending(batchSize);
    }
}