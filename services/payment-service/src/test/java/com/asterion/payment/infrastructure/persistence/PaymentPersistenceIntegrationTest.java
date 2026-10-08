package com.asterion.payment.infrastructure.persistence;

import com.asterion.payment.domain.model.Payment;
import com.asterion.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DataJpaTest
@Import(PaymentRepositoryAdapter.class)
class PaymentPersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("asterion_payment")
                    .withUsername("asterion")
                    .withPassword("asterion");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private PaymentRepositoryAdapter repositoryAdapter;

    @Autowired
    private SpringDataPaymentJpaRepository repository;

    @Test
    void shouldPersistAndFindPaymentByOrderId() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID sourceEventId = UUID.randomUUID();
        Instant now = Instant.now();

        Payment payment = Payment.initiate(
                paymentId,
                orderId,
                merchantId,
                customerId,
                sourceEventId,
                new BigDecimal("125.50"),
                now
        );

        boolean created = repositoryAdapter.createIfAbsent(payment);

        assertThat(created).isTrue();

        Payment loaded = repositoryAdapter.findByOrderId(orderId).orElseThrow();

        assertThat(loaded.paymentId()).isEqualTo(paymentId);
        assertThat(loaded.orderId()).isEqualTo(orderId);
        assertThat(loaded.merchantId()).isEqualTo(merchantId);
        assertThat(loaded.customerId()).isEqualTo(customerId);
        assertThat(loaded.sourceEventId()).isEqualTo(sourceEventId);
        assertThat(loaded.totalAmount()).isEqualByComparingTo("125.50");
        assertThat(loaded.status()).isEqualTo(PaymentStatus.INITIATED);
    }

    @Test
    void shouldFindPaymentBySourceEventId() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID sourceEventId = UUID.randomUUID();
        Instant now = Instant.now();

        Payment payment = Payment.initiate(
                paymentId,
                orderId,
                merchantId,
                customerId,
                sourceEventId,
                new BigDecimal("200.00"),
                now
        );

        repositoryAdapter.createIfAbsent(payment);

        Payment loaded =
                repositoryAdapter.findBySourceEventId(sourceEventId).orElseThrow();

        assertThat(loaded.paymentId()).isEqualTo(paymentId);
        assertThat(loaded.orderId()).isEqualTo(orderId);
        assertThat(loaded.sourceEventId()).isEqualTo(sourceEventId);
    }

    @Test
    void shouldReturnFalseWhenOrderAlreadyHasPayment() {
        UUID orderId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID firstPaymentId = UUID.randomUUID();
        UUID secondPaymentId = UUID.randomUUID();

        Payment firstPayment = Payment.initiate(
                firstPaymentId,
                orderId,
                merchantId,
                customerId,
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                Instant.now()
        );

        Payment secondPayment = Payment.initiate(
                secondPaymentId,
                orderId,
                merchantId,
                customerId,
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                Instant.now()
        );

        assertThat(repositoryAdapter.createIfAbsent(firstPayment)).isTrue();
        assertThat(repositoryAdapter.createIfAbsent(secondPayment)).isFalse();

        assertThat(repositoryAdapter.findByOrderId(orderId))
                .isPresent()
                .get()
                .extracting(Payment::paymentId)
                .isEqualTo(firstPaymentId);
    }

    @Test
    void shouldReturnFalseWhenSourceEventAlreadyExists() {
        UUID sourceEventId = UUID.randomUUID();

        Payment firstPayment = Payment.initiate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                sourceEventId,
                new BigDecimal("150.00"),
                Instant.now()
        );

        Payment secondPayment = Payment.initiate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                sourceEventId,
                new BigDecimal("175.00"),
                Instant.now()
        );

        assertThat(repositoryAdapter.createIfAbsent(firstPayment)).isTrue();

        // The adapter's ON CONFLICT(order_id) handles order-level idempotency,
        // while the database unique constraint protects source_event_id.
        assertThatThrownBy(() -> repositoryAdapter.createIfAbsent(secondPayment))
                .isInstanceOf(RuntimeException.class);
    }
}