package com.asterion.payment.application.port.in;

import java.util.UUID;

public interface ProcessPaymentUseCase {

    void process(UUID orderId);
}