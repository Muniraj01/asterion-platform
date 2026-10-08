package com.asterion.payment.infrastructure.payment;

import com.asterion.payment.application.model.PaymentProcessingResult;
import com.asterion.payment.domain.model.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StubPaymentProcessorTest {

    @Test
    void shouldReturnSuccessfulProcessingResult() {
        Payment payment = Payment.initiate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                Instant.parse("2026-10-07T10:00:00Z")
        );

        StubPaymentProcessor processor = new StubPaymentProcessor();

        assertEquals(
                PaymentProcessingResult.SUCCEEDED,
                processor.process(payment)
        );
    }
}