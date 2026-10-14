package com.asterion.payment.application.service;

import com.asterion.payment.application.port.in.ProcessPaymentUseCase;
import com.asterion.payment.application.port.out.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
public class PaymentRetryScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentRetryScheduler.class);

    private final PaymentRepository paymentRepository;
    private final ProcessPaymentUseCase processPaymentUseCase;
    private final Clock clock;
    private final int maxAttempts;
    private final int batchSize;

    public PaymentRetryScheduler(
            PaymentRepository paymentRepository,
            ProcessPaymentUseCase processPaymentUseCase,
            Clock clock,
            @Value("${asterion.payment.processing.max-attempts:5}")
            int maxAttempts,
            @Value("${asterion.payment.processing.recovery-batch-size:100}")
            int batchSize) {

        this.paymentRepository = paymentRepository;
        this.processPaymentUseCase = processPaymentUseCase;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString =
            "${asterion.payment.processing.recovery-interval-ms:5000}")
    public void recoverPayments() {
        Instant now = Instant.now(clock);

        int exhausted = paymentRepository
                .failExpiredProcessingBeyondMaxAttempts(now, maxAttempts);

        if (exhausted > 0) {
            log.error("Marked {} expired payment attempts as failed after "
                    + "reaching the retry limit", exhausted);
        }

        for (UUID orderId : paymentRepository.findDueOrderIds(
                now, maxAttempts, batchSize)) {
            try {
                processPaymentUseCase.process(orderId);
            } catch (RuntimeException exception) {
                // Continue the batch. The persisted lease and the next
                // scheduler run provide another recovery opportunity.
                log.error("Payment recovery failed for orderId={}", orderId, exception);
            }
        }
    }
}