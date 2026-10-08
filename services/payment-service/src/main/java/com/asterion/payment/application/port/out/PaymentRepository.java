package com.asterion.payment.application.port.out;

import com.asterion.payment.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Optional<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findBySourceEventId(UUID sourceEventId);

    boolean createIfAbsent(Payment payment);

    void save(Payment payment);
}