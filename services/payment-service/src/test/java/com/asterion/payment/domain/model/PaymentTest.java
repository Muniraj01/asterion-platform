package com.asterion.payment.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID MERCHANT_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-07T10:00:00Z");

    private Payment payment() {
        return Payment.initiate(
                PAYMENT_ID,
                ORDER_ID,
                MERCHANT_ID,
                CUSTOMER_ID,
                EVENT_ID,
                new BigDecimal("100.00"),
                NOW
        );
    }

    @Test
    void shouldStartInInitiatedState() {
        Payment payment = payment();

        assertEquals(PaymentStatus.INITIATED, payment.status());
        assertEquals(PAYMENT_ID, payment.paymentId());
        assertEquals(ORDER_ID, payment.orderId());
        assertEquals(MERCHANT_ID, payment.merchantId());
        assertEquals(CUSTOMER_ID, payment.customerId());
        assertEquals(EVENT_ID, payment.sourceEventId());
        assertEquals(new BigDecimal("100.00"), payment.totalAmount());
    }

    @Test
    void shouldTransitionFromInitiatedToProcessing() {
        Payment payment = payment();
        Instant processingAt = NOW.plusSeconds(10);

        payment.startProcessing(processingAt);

        assertEquals(PaymentStatus.PROCESSING, payment.status());
        assertEquals(processingAt, payment.updatedAt());
    }

    @Test
    void shouldTransitionFromInitiatedToFailed() {
        Payment payment = payment();
        Instant failedAt = NOW.plusSeconds(10);

        payment.fail(failedAt);

        assertEquals(PaymentStatus.FAILED, payment.status());
        assertEquals(failedAt, payment.updatedAt());
    }

    @Test
    void shouldTransitionFromProcessingToSucceeded() {
        Payment payment = payment();

        payment.startProcessing(NOW.plusSeconds(10));
        payment.succeed(NOW.plusSeconds(20));

        assertEquals(PaymentStatus.SUCCEEDED, payment.status());
    }

    @Test
    void shouldTransitionFromProcessingToFailed() {
        Payment payment = payment();

        payment.startProcessing(NOW.plusSeconds(10));
        payment.fail(NOW.plusSeconds(20));

        assertEquals(PaymentStatus.FAILED, payment.status());
    }

    @Test
    void shouldRejectSucceededToProcessing() {
        Payment payment = payment();

        payment.startProcessing(NOW.plusSeconds(10));
        payment.succeed(NOW.plusSeconds(20));

        assertThrows(
                InvalidPaymentStateTransitionException.class,
                () -> payment.startProcessing(NOW.plusSeconds(30))
        );
    }

    @Test
    void shouldRejectFailedToProcessing() {
        Payment payment = payment();

        payment.fail(NOW.plusSeconds(10));

        assertThrows(
                InvalidPaymentStateTransitionException.class,
                () -> payment.startProcessing(NOW.plusSeconds(20))
        );
    }

    @Test
    void shouldRejectSucceededToFailed() {
        Payment payment = payment();

        payment.startProcessing(NOW.plusSeconds(10));
        payment.succeed(NOW.plusSeconds(20));

        assertThrows(
                InvalidPaymentStateTransitionException.class,
                () -> payment.fail(NOW.plusSeconds(30))
        );
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Payment.initiate(
                        PAYMENT_ID,
                        ORDER_ID,
                        MERCHANT_ID,
                        CUSTOMER_ID,
                        EVENT_ID,
                        new BigDecimal("-1.00"),
                        NOW
                )
        );
    }
}