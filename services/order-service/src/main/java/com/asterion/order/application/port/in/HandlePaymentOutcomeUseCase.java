package com.asterion.order.application.port.in;

import com.asterion.events.PaymentFailedEvent;
import com.asterion.events.PaymentSucceededEvent;

public interface HandlePaymentOutcomeUseCase {

    void handle(PaymentSucceededEvent event);

    void handle(PaymentFailedEvent event);
}