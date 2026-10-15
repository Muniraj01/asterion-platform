
package com.asterion.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentOutboxJpaRepository
        extends JpaRepository<PaymentOutboxJpaEntity, UUID> {
}
