
package com.asterion.payment.infrastructure.messaging;

import com.asterion.payment.application.port.in.ProcessPaymentUseCase;
import com.asterion.payment.application.port.out.PaymentRepository;
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

        if (maxAttempts < 1 || batchSize <= 0)
            throw new IllegalArgumentException(
                    "Max attempts and recovery batch size must be positive");

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

        // Exhausted leases must transition to FAILED with an outcome event.
        paymentRepository
                .failExpiredProcessingBeyondMaxAttempts(now, maxAttempts);

        List<UUID> dueOrderIds = paymentRepository
                .findDueOrderIds(now, maxAttempts, batchSize);

        for (UUID orderId : dueOrderIds) {
            try {
                processPaymentUseCase.process(orderId);
            } catch (RuntimeException exception) {
                // A later recovery cycle can retry if processing or
                // persistence failed. The lease controls eligibility.
            }
        }
    }
}
