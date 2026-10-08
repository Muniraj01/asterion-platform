package com.asterion.payment.infrastructure.payment;

import com.asterion.payment.application.model.PaymentProcessingResult;
import com.asterion.payment.application.port.out.PaymentProcessorPort;
import com.asterion.payment.domain.model.Payment;
import org.springframework.stereotype.Component;

@Component
public class StubPaymentProcessor implements PaymentProcessorPort {

    @Override
    public PaymentProcessingResult process(Payment payment) {
        return PaymentProcessingResult.SUCCEEDED;
    }
}