
package com.asterion.payment.infrastructure.messaging;

import com.asterion.payment.application.port.in.ProcessPaymentUseCase;
import com.asterion.payment.application.port.out.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        prefix = "asterion.payment.processing",
        name = "recovery-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PaymentRecoveryScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentRecoveryScheduler.class);

    private final PaymentRepository paymentRepository;
    private final ProcessPaymentUseCase processPaymentUseCase;
    private final Clock clock;
    private final int maxAttempts;
    private final int batchSize;

    public PaymentRecoveryScheduler(
            PaymentRepository paymentRepository,
            ProcessPaymentUseCase processPaymentUseCase,
            Clock clock,
            @Value("${asterion.payment.processing.max-attempts:5}")
            int maxAttempts,
            @Value("${asterion.payment.processing.recovery-batch-size:100}")
            int batchSize) {

        if (maxAttempts < 1)
            throw new IllegalArgumentException("Max attempts must be positive");

        if (batchSize <= 0)
            throw new IllegalArgumentException("Recovery batch size must be positive");

        this.paymentRepository = paymentRepository;
        this.processPaymentUseCase = processPaymentUseCase;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString =
            "${asterion.payment.processing.recovery-interval-ms:5000}")
    public void recoverPayments() {
        Instant now = clock.instant();

        int exhaustedCount = paymentRepository
                .failExpiredProcessingBeyondMaxAttempts(now, maxAttempts);

        if (exhaustedCount > 0) {
            log.warn("Marked {} expired payment(s) as failed after reaching "
                    + "the maximum of {} attempts", exhaustedCount, maxAttempts);
        }

        List<UUID> dueOrderIds = paymentRepository
                .findDueOrderIds(now, maxAttempts, batchSize);

        if (dueOrderIds.isEmpty())
            return;

        log.debug("Starting payment recovery for {} order(s)", dueOrderIds.size());

        int failureCount = 0;

        for (UUID orderId : dueOrderIds) {
            try {
                processPaymentUseCase.process(orderId);
            } catch (RuntimeException exception) {
                failureCount++;

                // Keep processing the rest of the batch. The persisted
                // processing lease governs eligibility for a later retry.
                log.error("Payment recovery failed for orderId={}; "
                        + "a later recovery cycle may retry it", orderId, exception);
            }
        }

        if (failureCount > 0) {
            log.warn("Payment recovery batch completed with {} failure(s) "
                    + "out of {} order(s)", failureCount, dueOrderIds.size());
        } else {
            log.debug("Payment recovery batch completed successfully for "
                    + "{} order(s)", dueOrderIds.size());
        }
    }
}
