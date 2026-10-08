package com.asterion.payment.application.service;

import com.asterion.payment.application.model.PaymentInitiation;
import com.asterion.payment.application.port.in.InitiatePaymentUseCase;
import com.asterion.payment.application.port.in.ProcessPaymentUseCase;
import com.asterion.payment.application.port.out.PaymentRepository;
import com.asterion.payment.domain.model.Payment;
import com.asterion.payment.domain.model.PaymentStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class InitiatePaymentService implements InitiatePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final ProcessPaymentUseCase processPaymentUseCase;

    public InitiatePaymentService(
            PaymentRepository paymentRepository,
            ProcessPaymentUseCase processPaymentUseCase) {
        this.paymentRepository = paymentRepository;
        this.processPaymentUseCase = processPaymentUseCase;
    }

    @Override
    @Transactional
    public void initiate(PaymentInitiation paymentInitiation) {
        if (paymentRepository.findBySourceEventId(
                paymentInitiation.eventId()).isPresent())
            return;

        Payment existingPayment = paymentRepository
                .findByOrderId(paymentInitiation.orderId())
                .orElse(null);

        if (existingPayment != null) {
            if (existingPayment.status() == PaymentStatus.INITIATED) {
                processPaymentUseCase.process(existingPayment.orderId());
            }
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

        boolean created = paymentRepository.createIfAbsent(payment);

        if (created)
            processPaymentUseCase.process(payment.orderId());
    }
}