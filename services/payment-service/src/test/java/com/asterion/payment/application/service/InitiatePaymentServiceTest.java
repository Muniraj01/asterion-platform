package com.asterion.payment.application.service;

import com.asterion.payment.application.model.PaymentInitiation;
import com.asterion.payment.application.port.in.ProcessPaymentUseCase;
import com.asterion.payment.application.port.out.PaymentRepository;
import com.asterion.payment.domain.model.Payment;
import com.asterion.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitiatePaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ProcessPaymentUseCase processPaymentUseCase;

    private InitiatePaymentService service;

    private UUID eventId;
    private UUID orderId;
    private UUID merchantId;
    private UUID customerId;
    private Instant occurredAt;

    @BeforeEach
    void setUp() {
        service = new InitiatePaymentService(paymentRepository, processPaymentUseCase);
        eventId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        merchantId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        occurredAt = Instant.parse("2026-10-07T10:00:00Z");
    }

    private PaymentInitiation initiation() {
        return new PaymentInitiation(
                eventId,
                occurredAt,
                orderId,
                merchantId,
                customerId,
                new BigDecimal("250.5000")
        );
    }

    @Test
    void shouldCreatePaymentForNewInitiation() {
        when(paymentRepository.findBySourceEventId(eventId))
                .thenReturn(Optional.empty());

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.empty());

        when(paymentRepository.createIfAbsent(any(Payment.class)))
                .thenReturn(true);

        service.initiate(initiation());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).createIfAbsent(captor.capture());

        Payment payment = captor.getValue();

        assertNotNull(payment.paymentId());
        assertEquals(orderId, payment.orderId());
        assertEquals(merchantId, payment.merchantId());
        assertEquals(customerId, payment.customerId());
        assertEquals(eventId, payment.sourceEventId());
        assertEquals(new BigDecimal("250.5000"), payment.totalAmount());
        assertEquals(PaymentStatus.INITIATED, payment.status());
        assertEquals(occurredAt, payment.createdAt());

        verify(processPaymentUseCase).process(orderId);
    }

    @Test
    void shouldIgnoreDuplicateSourceEvent() {
        Payment existingPayment = Payment.initiate(
                UUID.randomUUID(),
                orderId,
                merchantId,
                customerId,
                eventId,
                new BigDecimal("250.5000"),
                occurredAt
        );

        when(paymentRepository.findBySourceEventId(eventId))
                .thenReturn(Optional.of(existingPayment));

        service.initiate(initiation());

        verify(paymentRepository, never()).findByOrderId(any());
        verify(paymentRepository, never()).createIfAbsent(any());
        verifyNoInteractions(processPaymentUseCase);
    }

    @Test
    void shouldProcessExistingInitiatedPayment() {
        Payment existingPayment = Payment.initiate(
                UUID.randomUUID(),
                orderId,
                merchantId,
                customerId,
                UUID.randomUUID(),
                new BigDecimal("250.5000"),
                occurredAt
        );

        when(paymentRepository.findBySourceEventId(eventId))
                .thenReturn(Optional.empty());

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(existingPayment));

        service.initiate(initiation());

        verify(processPaymentUseCase).process(orderId);
        verify(paymentRepository, never()).createIfAbsent(any());
    }

    @Test
    void shouldNotCreatePaymentWhenRepositoryReportsExistingOrder() {
        when(paymentRepository.findBySourceEventId(eventId))
                .thenReturn(Optional.empty());

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.empty());

        when(paymentRepository.createIfAbsent(any(Payment.class)))
                .thenReturn(false);

        service.initiate(initiation());
        verify(paymentRepository).createIfAbsent(any(Payment.class));
        verify(processPaymentUseCase, never()).process(any());
    }
}