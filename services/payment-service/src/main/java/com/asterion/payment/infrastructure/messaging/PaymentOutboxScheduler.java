
package com.asterion.payment.infrastructure.messaging;

import com.asterion.payment.application.service.PaymentOutboxPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "asterion.payment.outbox",
        name = "scheduler-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PaymentOutboxScheduler {

    private final PaymentOutboxPublisher publisher;
    private final int batchSize;

    public PaymentOutboxScheduler(
            PaymentOutboxPublisher publisher,
            @Value("${asterion.payment.outbox.batch-size:100}")
            int batchSize) {

        if (batchSize <= 0)
            throw new IllegalArgumentException(
                    "Outbox batch size must be greater than zero");

        this.publisher = publisher;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${asterion.payment.outbox.poll-interval-ms:1000}")
    public void publishPendingEvents() {
        publisher.publishPending(batchSize);
    }
}
