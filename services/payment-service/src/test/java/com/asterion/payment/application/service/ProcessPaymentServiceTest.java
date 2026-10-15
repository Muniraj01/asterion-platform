
package com.asterion.payment.application.service;

import com.asterion.events.PaymentFailedEvent;
import com.asterion.events.PaymentSucceededEvent;
import com.asterion.payment.application.model.PaymentProcessingResult;
import com.asterion.payment.application.port.out.PaymentProcessorPort;
import com.asterion.payment.application.port.out.PaymentRepository;
import com.asterion.payment.domain.model.Payment;
import com.asterion.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentServiceTest {

    private static final int MAX_ATTEMPTS = 5;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentProcessorPort paymentProcessorPort;

    private ProcessPaymentService service;

    private UUID paymentId;
    private UUID orderId;
    private UUID merchantId;
    private UUID customerId;
    private UUID eventId;

    private Instant createdAt;
    private Instant now;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        merchantId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        createdAt = Instant.parse("2026-10-07T10:00:00Z");
        now = Instant.parse("2026-10-07T10:00:20Z");

        service = new ProcessPaymentService(
                paymentRepository,
                paymentProcessorPort,
                Clock.fixed(now, ZoneOffset.UTC),
                MAX_ATTEMPTS,
                Duration.ofSeconds(5),
                Duration.ofMinutes(5),
                Duration.ofSeconds(60));
    }

    private Payment claimedPayment(int attemptCount) {
        return Payment.rehydrate(
                paymentId,
                orderId,
                merchantId,
                customerId,
                eventId,
                new BigDecimal("250.00"),
                PaymentStatus.PROCESSING,
                createdAt,
                now,
                attemptCount,
                null,
                now.plusSeconds(60));
    }

    private void claim(Payment payment) {
        when(paymentRepository.claimForProcessing(
                eq(orderId),
                eq(now),
                eq(now.plusSeconds(60)),
                eq(MAX_ATTEMPTS)))
                .thenReturn(Optional.of(payment));
    }

    @Test
    void shouldPersistSuccessfulOutcomeWithEvent() {
        Payment payment = claimedPayment(1);
        claim(payment);

        when(paymentProcessorPort.process(payment, paymentId.toString()))
                .thenReturn(PaymentProcessingResult.SUCCEEDED);

        service.process(orderId);

        assertEquals(PaymentStatus.SUCCEEDED, payment.status());
        assertEquals(now, payment.updatedAt());

        verify(paymentRepository).saveWithOutcome(
                eq(payment), isA(PaymentSucceededEvent.class));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldPersistFailedOutcomeWithEvent() {
        Payment payment = claimedPayment(1);
        claim(payment);

        when(paymentProcessorPort.process(payment, paymentId.toString()))
                .thenReturn(PaymentProcessingResult.FAILED);

        service.process(orderId);

        assertEquals(PaymentStatus.FAILED, payment.status());

        verify(paymentRepository).saveWithOutcome(
                eq(payment), isA(PaymentFailedEvent.class));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldScheduleRetryWithoutTerminalEvent() {
        Payment payment = claimedPayment(1);
        claim(payment);

        when(paymentProcessorPort.process(payment, paymentId.toString()))
                .thenThrow(new IllegalStateException("processor unavailable"));

        service.process(orderId);

        assertEquals(PaymentStatus.RETRYABLE, payment.status());
        assertEquals(now.plusSeconds(5), payment.nextAttemptAt());
        assertNull(payment.processingLeaseUntil());

        verify(paymentRepository).save(payment);
        verify(paymentRepository, never()).saveWithOutcome(any(), any());
    }

    @Test
    void shouldEmitFailureEventWhenTechnicalRetriesAreExhausted() {
        Payment payment = claimedPayment(MAX_ATTEMPTS);
        claim(payment);

        when(paymentProcessorPort.process(payment, paymentId.toString()))
                .thenThrow(new IllegalStateException("processor unavailable"));

        service.process(orderId);

        assertEquals(PaymentStatus.FAILED, payment.status());
        assertNull(payment.nextAttemptAt());

        verify(paymentRepository).saveWithOutcome(
                eq(payment), isA(PaymentFailedEvent.class));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldNotCallProcessorWhenClaimIsUnavailable() {
        when(paymentRepository.claimForProcessing(
                eq(orderId),
                eq(now),
                eq(now.plusSeconds(60)),
                eq(MAX_ATTEMPTS)))
                .thenReturn(Optional.empty());

        service.process(orderId);

        verifyNoInteractions(paymentProcessorPort);
        verify(paymentRepository, never()).save(any());
        verify(paymentRepository, never()).saveWithOutcome(any(), any());
    }

    @Test
    void shouldPropagateFailureToPersistTerminalOutcome() {
        Payment payment = claimedPayment(1);
        claim(payment);

        when(paymentProcessorPort.process(payment, paymentId.toString()))
                .thenReturn(PaymentProcessingResult.SUCCEEDED);

        doThrow(new IllegalStateException("database unavailable"))
                .when(paymentRepository)
                .saveWithOutcome(eq(payment), any(PaymentSucceededEvent.class));

        assertThrows(IllegalStateException.class, () -> service.process(orderId));
        assertEquals(PaymentStatus.SUCCEEDED, payment.status());
    }
}
