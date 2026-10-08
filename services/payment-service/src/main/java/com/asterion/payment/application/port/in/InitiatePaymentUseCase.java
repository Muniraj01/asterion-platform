package com.asterion.payment.application.port.in;

import com.asterion.payment.application.model.PaymentInitiation;

public interface InitiatePaymentUseCase {

    void initiate(PaymentInitiation paymentInitiation);
}