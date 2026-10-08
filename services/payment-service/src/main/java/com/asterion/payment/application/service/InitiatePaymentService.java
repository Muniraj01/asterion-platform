package com.asterion.payment.application.service;

import com.asterion.payment.application.model.PaymentInitiation;
import com.asterion.payment.application.port.in.InitiatePaymentUseCase;
import com.asterion.payment.application.port.out.PaymentRepository;
import com.asterion.payment.domain.model.Payment;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class InitiatePaymentService implements InitiatePaymentUseCase {

    private final PaymentRepository paymentRepository;

    public InitiatePaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public void initiate(PaymentInitiation paymentInitiation) {

        if (paymentRepository.findBySourceEventId(
                paymentInitiation.eventId()).isPresent()) {
            return;
        }

        if (paymentRepository.findByOrderId(
                paymentInitiation.orderId()).isPresent()) {
            return;
        }

        Payment payment = Payment.initiate(
                UUID.randomUUID(),
                paymentInitiation.orderId(),
                paymentInitiation.merchantId(),
                paymentInitiation.customerId(),
                paymentInitiation.eventId(),
                paymentInitiation.totalAmount(),
                paymentInitiation.occurredAt()
        );

        paymentRepository.createIfAbsent(payment);
    }
}