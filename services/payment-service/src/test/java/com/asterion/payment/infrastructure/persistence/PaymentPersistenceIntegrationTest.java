package com.asterion.payment.infrastructure.persistence;

import com.asterion.events.PaymentSucceededEvent;
import com.asterion.payment.domain.model.Payment;
import com.asterion.payment.domain.model.PaymentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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
@Import({
        PaymentRepositoryAdapter.class,
        PaymentPersistenceIntegrationTest.JacksonTestConfiguration.class
})
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

    @Autowired
    private PaymentOutboxJpaRepository outboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @Test
    void shouldPersistTerminalPaymentAndOutcomeEventTogether() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID sourceEventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-14T10:00:00Z");

        Payment initiated = Payment.initiate(
                paymentId,
                orderId,
                merchantId,
                customerId,
                sourceEventId,
                new BigDecimal("125.50"),
                now
        );

        assertThat(repositoryAdapter.createIfAbsent(initiated)).isTrue();

        Payment processing = repositoryAdapter.claimForProcessing(
                        orderId, now, now.plusSeconds(60), 5)
                .orElseThrow();

        Instant completedAt = now.plusSeconds(2);
        processing.succeed(completedAt);

        UUID outcomeEventId = UUID.randomUUID();
        PaymentSucceededEvent event = new PaymentSucceededEvent(
                outcomeEventId,
                completedAt,
                paymentId,
                orderId,
                merchantId,
                customerId,
                new BigDecimal("125.50")
        );

        repositoryAdapter.saveWithOutcome(processing, event);

        assertThat(repositoryAdapter.findByOrderId(orderId))
                .isPresent()
                .get()
                .extracting(Payment::status)
                .isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(outboxRepository.findById(outcomeEventId))
                .isPresent()
                .get()
                .satisfies(outbox -> {
                    assertThat(outbox.getEventType())
                            .isEqualTo("payment.succeeded.v1");
                    assertThat(outbox.getStatus()).isEqualTo("NEW");
                    assertThat(outbox.getPayload())
                            .contains(outcomeEventId.toString())
                            .contains(paymentId.toString())
                            .contains(orderId.toString());
                });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRollbackTerminalPaymentUpdateWhenOutboxInsertFails() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID sourceEventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-14T10:00:00Z");

        Payment initiated = Payment.initiate(
                paymentId,
                orderId,
                merchantId,
                customerId,
                sourceEventId,
                new BigDecimal("125.50"),
                now
        );

        assertThat(repositoryAdapter.createIfAbsent(initiated)).isTrue();

        Payment processing = repositoryAdapter.claimForProcessing(
                        orderId, now, now.plusSeconds(60), 5)
                .orElseThrow();

        assertThat(processing.status()).isEqualTo(PaymentStatus.PROCESSING);

        Instant completedAt = now.plusSeconds(2);
        processing.succeed(completedAt);

        UUID outcomeEventId = UUID.randomUUID();
        PaymentSucceededEvent event = new PaymentSucceededEvent(
                outcomeEventId,
                completedAt,
                paymentId,
                orderId,
                merchantId,
                customerId,
                new BigDecimal("125.50")
        );

        /*
         * The trigger simulates a database-level outbox insert failure.
         * Because this test runs without a test-managed transaction,
         * the repository adapter's actual @Transactional boundary is tested.
         */
        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION reject_payment_outbox_insert()
                RETURNS trigger
                LANGUAGE plpgsql
                AS $$
                BEGIN
                    RAISE EXCEPTION 'forced payment outbox insert failure';
                END;
                $$
                """);

        try {
            jdbcTemplate.execute("""
                    CREATE TRIGGER trg_test_reject_payment_outbox_insert
                    BEFORE INSERT ON payment_outbox_events
                    FOR EACH ROW
                    EXECUTE FUNCTION reject_payment_outbox_insert()
                    """);

            assertThatThrownBy(
                    () -> repositoryAdapter.saveWithOutcome(processing, event)
            ).isInstanceOf(RuntimeException.class);

            /*
             * The adapter call must have rolled back before these fresh
             * repository reads begin. Neither write may have committed.
             */
            Payment reloaded = repositoryAdapter.findByOrderId(orderId)
                    .orElseThrow();

            assertThat(reloaded.status())
                    .as("payment status must roll back when outbox insertion fails")
                    .isEqualTo(PaymentStatus.PROCESSING);

            assertThat(outboxRepository.findById(outcomeEventId))
                    .as("the failed outbox insert must not leave an outcome event")
                    .isEmpty();
        } finally {
            jdbcTemplate.execute("""
                    DROP TRIGGER IF EXISTS
                    trg_test_reject_payment_outbox_insert
                    ON payment_outbox_events
                    """);

            jdbcTemplate.execute("""
                    DROP FUNCTION IF EXISTS reject_payment_outbox_insert()
                    """);

            repository.deleteById(paymentId);
        }
    }

    @TestConfiguration
    static class JacksonTestConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }
    }
}