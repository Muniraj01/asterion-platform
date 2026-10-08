package com.asterion.payment.application.service;

import com.asterion.payment.application.model.PaymentProcessingResult;
import com.asterion.payment.application.port.in.ProcessPaymentUseCase;
import com.asterion.payment.application.port.out.PaymentProcessorPort;
import com.asterion.payment.application.port.out.PaymentRepository;
import com.asterion.payment.domain.model.Payment;
import com.asterion.payment.domain.model.PaymentStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public class ProcessPaymentService implements ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentProcessorPort paymentProcessorPort;
    private final Clock clock;

    public ProcessPaymentService(
            PaymentRepository paymentRepository,
            PaymentProcessorPort paymentProcessorPort,
            Clock clock) {
        this.paymentRepository = paymentRepository;
        this.paymentProcessorPort = paymentProcessorPort;
        this.clock = clock;
    }

    @Transactional
    public void process(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "Payment not found for order: " + orderId));

        if (payment.status() != PaymentStatus.INITIATED)
            return;

        Instant now = Instant.now(clock);
        payment.startProcessing(now);
        paymentRepository.save(payment);

        PaymentProcessingResult result;
        try {
            result = paymentProcessorPort.process(payment);
        } catch (RuntimeException exception) {
            payment.fail(Instant.now(clock));
            paymentRepository.save(payment);
            throw exception;
        }

        if (result == PaymentProcessingResult.SUCCEEDED) {
            payment.succeed(Instant.now(clock));
        } else {
            payment.fail(Instant.now(clock));
        }

        paymentRepository.save(payment);
    }
}