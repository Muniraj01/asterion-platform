package com.asterion.payment.application.port.out;

import com.asterion.payment.application.model.PaymentOutboxEvent;

public interface PaymentOutcomeEventPublisher {

    void publish(PaymentOutboxEvent event);
}
