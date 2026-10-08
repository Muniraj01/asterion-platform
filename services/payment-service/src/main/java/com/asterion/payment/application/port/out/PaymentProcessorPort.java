package com.asterion.payment.application.port.out;

import com.asterion.payment.application.model.PaymentProcessingResult;
import com.asterion.payment.domain.model.Payment;

public interface PaymentProcessorPort {

    PaymentProcessingResult process(Payment payment);
}