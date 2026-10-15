
package com.asterion.payment.application.service;

import com.asterion.events.DomainEvent;
import com.asterion.events.PaymentFailedEvent;
import com.asterion.events.PaymentSucceededEvent;
import com.asterion.payment.application.model.PaymentProcessingResult;
import com.asterion.payment.application.port.in.ProcessPaymentUseCase;
import com.asterion.payment.application.port.out.PaymentProcessorPort;
import com.asterion.payment.application.port.out.PaymentRepository;
import com.asterion.payment.domain.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class ProcessPaymentService implements ProcessPaymentUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(ProcessPaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentProcessorPort paymentProcessorPort;
    private final Clock clock;
    private final int maxAttempts;
    private final Duration baseRetryDelay;
    private final Duration maxRetryDelay;
    private final Duration processingLeaseDuration;

    public ProcessPaymentService(
            PaymentRepository paymentRepository,
            PaymentProcessorPort paymentProcessorPort,
            Clock clock,
            int maxAttempts,
            Duration baseRetryDelay,
            Duration maxRetryDelay,
            Duration processingLeaseDuration) {

        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "Maximum attempts must be at least one");
        }
        if (baseRetryDelay.isNegative() || baseRetryDelay.isZero()) {
            throw new IllegalArgumentException(
                    "Base retry delay must be positive");
        }
        if (maxRetryDelay.compareTo(baseRetryDelay) < 0) {
            throw new IllegalArgumentException(
                    "Maximum retry delay must not be less than base delay");
        }
        if (processingLeaseDuration.isNegative()
                || processingLeaseDuration.isZero()) {
            throw new IllegalArgumentException(
                    "Processing lease duration must be positive");
        }

        this.paymentRepository = paymentRepository;
        this.paymentProcessorPort = paymentProcessorPort;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.baseRetryDelay = baseRetryDelay;
        this.maxRetryDelay = maxRetryDelay;
        this.processingLeaseDuration = processingLeaseDuration;
    }

    @Override
    public void process(UUID orderId) {
        Instant now = clock.instant();
        Instant leaseUntil = now.plus(processingLeaseDuration);

        Payment payment = paymentRepository
                .claimForProcessing(orderId, now, leaseUntil, maxAttempts)
                .orElse(null);

        if (payment == null)
            return;

        PaymentProcessingResult result;
        try {
            result = paymentProcessorPort.process(
                    payment, payment.paymentId().toString());
        } catch (RuntimeException exception) {
            recordTechnicalFailure(payment, exception);
            return;
        }

        Instant completedAt = clock.instant();

        if (result == PaymentProcessingResult.SUCCEEDED) {
            payment.succeed(completedAt);
            paymentRepository.saveWithOutcome(
                    payment, succeededEvent(payment, completedAt));
        } else {
            payment.fail(completedAt);
            paymentRepository.saveWithOutcome(
                    payment, failedEvent(payment, completedAt));
        }
    }

    private void recordTechnicalFailure(
            Payment payment, RuntimeException exception) {
        Instant now = clock.instant();

        if (payment.attemptCount() >= maxAttempts) {
            payment.fail(now);

            log.error("Payment processing exhausted its retry limit. "
                            + "paymentId={}, orderId={}, attempts={}",
                    payment.paymentId(),
                    payment.orderId(),
                    payment.attemptCount(),
                    exception);

            paymentRepository.saveWithOutcome(payment, failedEvent(payment, now));
        } else {
            Instant retryAt = now.plus(retryDelay(payment.attemptCount()));
            payment.scheduleRetry(retryAt, now);

            log.warn("Payment processing failed; retry scheduled. "
                            + "paymentId={}, orderId={}, attempts={}, retryAt={}",
                    payment.paymentId(),
                    payment.orderId(),
                    payment.attemptCount(),
                    retryAt,
                    exception);

            // Intermediate failures do not produce terminal outcome events.
            paymentRepository.save(payment);
        }
    }

    private PaymentSucceededEvent succeededEvent(
            Payment payment, Instant occurredAt) {
        return new PaymentSucceededEvent(
                UUID.randomUUID(),
                occurredAt,
                payment.paymentId(),
                payment.orderId(),
                payment.merchantId(),
                payment.customerId(),
                payment.totalAmount());
    }

    private PaymentFailedEvent failedEvent(
            Payment payment, Instant occurredAt) {
        return new PaymentFailedEvent(
                UUID.randomUUID(),
                occurredAt,
                payment.paymentId(),
                payment.orderId(),
                payment.merchantId(),
                payment.customerId(),
                payment.totalAmount());
    }

    private Duration retryDelay(int attemptCount) {
        long multiplier = 1L << Math.min(attemptCount - 1, 30);

        Duration delay;
        try {
            delay = baseRetryDelay.multipliedBy(multiplier);
        } catch (ArithmeticException exception) {
            return maxRetryDelay;
        }

        return delay.compareTo(maxRetryDelay) > 0 ? maxRetryDelay : delay;
    }
}
