package com.asterion.payment.infrastructure.persistence;

import com.asterion.payment.application.port.out.PaymentRepository;
import com.asterion.payment.domain.model.Payment;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final SpringDataPaymentJpaRepository repository;

    public PaymentRepositoryAdapter(SpringDataPaymentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId)
                .map(PaymentJpaEntity::toDomain);
    }

    @Override
    public Optional<Payment> findBySourceEventId(UUID sourceEventId) {
        return repository.findBySourceEventId(sourceEventId)
                .map(PaymentJpaEntity::toDomain);
    }

    @Override
    public boolean createIfAbsent(Payment payment) {
        return repository.insertIfAbsent(
                payment.paymentId(),
                payment.orderId(),
                payment.merchantId(),
                payment.customerId(),
                payment.sourceEventId(),
                payment.totalAmount(),
                payment.status().name(),
                payment.createdAt(),
                payment.updatedAt()
        ) == 1;
    }
}