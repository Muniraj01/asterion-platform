
package com.asterion.payment.infrastructure.messaging;

import com.asterion.payment.application.port.in.ProcessPaymentUseCase;
import com.asterion.payment.application.port.out.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentRecoverySchedulerTest {

    private static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");

    private static final int MAX_ATTEMPTS = 5;
    private static final int BATCH_SIZE = 100;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ProcessPaymentUseCase processPaymentUseCase;

    private Clock clock;
    private PaymentRecoveryScheduler scheduler;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        scheduler = new PaymentRecoveryScheduler(
                paymentRepository,
                processPaymentUseCase,
                clock,
                MAX_ATTEMPTS,
                BATCH_SIZE);
    }

    @Test
    void shouldFailExhaustedPaymentsAndRecoverDueOrders() {
        UUID firstOrderId = UUID.randomUUID();
        UUID secondOrderId = UUID.randomUUID();

        when(paymentRepository
                .failExpiredProcessingBeyondMaxAttempts(NOW, MAX_ATTEMPTS))
                .thenReturn(2);

        when(paymentRepository
                .findDueOrderIds(NOW, MAX_ATTEMPTS, BATCH_SIZE))
                .thenReturn(List.of(firstOrderId, secondOrderId));

        scheduler.recoverPayments();

        verify(paymentRepository)
                .failExpiredProcessingBeyondMaxAttempts(NOW, MAX_ATTEMPTS);
        verify(paymentRepository).findDueOrderIds(NOW, MAX_ATTEMPTS, BATCH_SIZE);
        verify(processPaymentUseCase).process(firstOrderId);
        verify(processPaymentUseCase).process(secondOrderId);
        verifyNoMoreInteractions(processPaymentUseCase);
    }

    @Test
    void shouldContinueRecoveringWhenOneOrderFails() {
        UUID failingOrderId = UUID.randomUUID();
        UUID succeedingOrderId = UUID.randomUUID();

        when(paymentRepository
                .failExpiredProcessingBeyondMaxAttempts(NOW, MAX_ATTEMPTS))
                .thenReturn(0);

        when(paymentRepository
                .findDueOrderIds(NOW, MAX_ATTEMPTS, BATCH_SIZE))
                .thenReturn(List.of(failingOrderId, succeedingOrderId));

        doThrow(new IllegalStateException("Simulated processing failure"))
                .when(processPaymentUseCase).process(failingOrderId);

        scheduler.recoverPayments();

        verify(processPaymentUseCase).process(failingOrderId);
        verify(processPaymentUseCase).process(succeedingOrderId);
    }

    @Test
    void shouldStopAfterRepositoryReturnsNoDueOrders() {
        when(paymentRepository
                .failExpiredProcessingBeyondMaxAttempts(NOW, MAX_ATTEMPTS))
                .thenReturn(0);

        when(paymentRepository.findDueOrderIds(NOW, MAX_ATTEMPTS, BATCH_SIZE))
                .thenReturn(List.of());

        scheduler.recoverPayments();

        verify(processPaymentUseCase, never())
                .process(org.mockito.ArgumentMatchers.any(UUID.class));
    }

    @Test
    void shouldRejectInvalidMaxAttempts() {
        assertThrows(IllegalArgumentException.class,
                () -> new PaymentRecoveryScheduler(
                        paymentRepository,
                        processPaymentUseCase,
                        clock,
                        0,
                        BATCH_SIZE));
    }

    @Test
    void shouldRejectInvalidBatchSize() {
        assertThrows(IllegalArgumentException.class,
                () -> new PaymentRecoveryScheduler(
                        paymentRepository,
                        processPaymentUseCase,
                        clock,
                        MAX_ATTEMPTS,
                        0));
    }
}
