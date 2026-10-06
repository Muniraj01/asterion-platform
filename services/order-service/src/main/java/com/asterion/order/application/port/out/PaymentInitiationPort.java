package com.asterion.order.application.port.out;

import com.asterion.order.application.model.PaymentInitiation;

public interface PaymentInitiationPort {

    void initiate(PaymentInitiation paymentInitiation);
}