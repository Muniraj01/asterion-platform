package com.asterion.payment.application.service;

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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentProcessorPort paymentProcessorPort;

    private ProcessPaymentService service;

    private UUID paymentId;
    private UUID orderId;
    private Instant createdAt;
    private Instant processingAt;
    private Instant completedAt;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        createdAt = Instant.parse("2026-10-07T10:00:00Z");
        processingAt = Instant.parse("2026-10-07T10:00:10Z");
        completedAt = Instant.parse("2026-10-07T10:00:20Z");

        Clock clock = Clock.fixed(completedAt, ZoneOffset.UTC);
        service = new ProcessPaymentService(
                paymentRepository, paymentProcessorPort, clock);
    }

    private Payment payment() {
        return Payment.initiate(
                paymentId,
                orderId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("250.00"),
                createdAt
        );
    }

    @Test
    void shouldProcessPaymentSuccessfully() {
        Payment payment = payment();

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(payment));

        when(paymentProcessorPort.process(payment))
                .thenReturn(PaymentProcessingResult.SUCCEEDED);

        service.process(orderId);

        assertEquals(PaymentStatus.SUCCEEDED, payment.status());
        assertEquals(completedAt, payment.updatedAt());

        verify(paymentRepository, atLeast(2)).save(payment);
        verify(paymentProcessorPort).process(payment);
    }

    @Test
    void shouldMarkPaymentFailedWhenProcessorReturnsFailure() {
        Payment payment = payment();

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(payment));

        when(paymentProcessorPort.process(payment))
                .thenReturn(PaymentProcessingResult.FAILED);

        service.process(orderId);

        assertEquals(PaymentStatus.FAILED, payment.status());
        verify(paymentProcessorPort).process(payment);
        verify(paymentRepository, atLeast(2)).save(payment);
    }

    @Test
    void shouldNotProcessAlreadySucceededPayment() {
        Payment payment = payment();
        payment.startProcessing(processingAt);
        payment.succeed(completedAt);

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(payment));

        service.process(orderId);

        verifyNoInteractions(paymentProcessorPort);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldNotProcessAlreadyFailedPayment() {
        Payment payment = payment();
        payment.fail(processingAt);

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(payment));

        service.process(orderId);

        verifyNoInteractions(paymentProcessorPort);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldFailPaymentWhenProcessorThrows() {
        Payment payment = payment();

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(payment));

        RuntimeException processorFailure =
                new IllegalStateException("processor unavailable");

        when(paymentProcessorPort.process(payment))
                .thenThrow(processorFailure);

        assertThrows(RuntimeException.class, () -> service.process(orderId));

        assertEquals(PaymentStatus.FAILED, payment.status());
        verify(paymentRepository, atLeast(2)).save(payment);
    }

    @Test
    void shouldFailWhenPaymentDoesNotExist() {
        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.process(orderId));

        verifyNoInteractions(paymentProcessorPort);
    }
}