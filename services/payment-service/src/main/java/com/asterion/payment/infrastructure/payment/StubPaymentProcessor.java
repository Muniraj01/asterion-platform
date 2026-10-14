package com.asterion.payment.infrastructure.payment;

import com.asterion.payment.application.model.PaymentProcessingResult;
import com.asterion.payment.application.port.out.PaymentProcessorPort;
import com.asterion.payment.domain.model.Payment;
import org.springframework.stereotype.Component;

@Component
public class StubPaymentProcessor implements PaymentProcessorPort {

    @Override
    public PaymentProcessingResult process(Payment payment, String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank())
            throw new IllegalArgumentException(
                    "Payment idempotency key must not be blank");

        // Simulated successful response.
        // A real provider adapter must send this key to the provider and
        // map provider-specific outcomes to PaymentProcessingResult.
        return PaymentProcessingResult.SUCCEEDED;
    }
}