package com.asterion.merchant.infrastructure.scheduling;

import com.asterion.merchant.application.service.MerchantOutboxPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;

@ConditionalOnProperty(prefix = "asterion.merchant.outbox", name = "scheduler-enabled",
        havingValue = "true", matchIfMissing = true)
public class MerchantOutboxScheduler {

    private final MerchantOutboxPublisher merchantOutboxPublisher;
    private final int batchSize;

    public MerchantOutboxScheduler(
            MerchantOutboxPublisher merchantOutboxPublisher,
            @Value("${asterion.merchant.outbox.batch-size:100}") int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be greater than zero");
        }
        this.merchantOutboxPublisher = merchantOutboxPublisher;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${asterion.merchant.outbox.poll-interval-ms:1000}")
    public void publishPendingEvents() {
        merchantOutboxPublisher.publishPending(batchSize);
    }
}