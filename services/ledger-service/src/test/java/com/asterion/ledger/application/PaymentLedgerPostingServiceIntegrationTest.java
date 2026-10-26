
package com.asterion.ledger.application;

import com.asterion.events.PaymentSucceededEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {"spring.kafka.listener.auto-startup=false"})
@Testcontainers
class PaymentLedgerPostingServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("asterion_ledger_test")
                    .withUsername("asterion")
                    .withPassword("asterion");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
    }

    @Autowired
    private PaymentLedgerPostingService postingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                DROP TRIGGER IF EXISTS trg_reject_merchant_payable_entry
                ON ledger_entries
                """);

        jdbcTemplate.execute("""
                DROP FUNCTION IF EXISTS reject_merchant_payable_entry()
                """);

        jdbcTemplate.execute("""
                TRUNCATE TABLE ledger_entries, ledger_transactions
                """);
    }

    @Test
    void persistsBalancedDoubleEntryJournalForSuccessfulPayment() {
        PaymentSucceededEvent event = successfulPayment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("1000.0000")
        );

        postingService.postSuccessfulPayment(event);

        assertEquals(1L, countTransactions(event.paymentId()));
        assertEquals(2L, countEntries(event.paymentId()));

        assertEquals(1000.0000, sumAmount(event.paymentId(),
                "debit_amount").doubleValue(), 0.0001);
        assertEquals(1000.0000, sumAmount(event.paymentId(),
                "credit_amount").doubleValue(), 0.0001);

        assertEquals(1L, countEntriesForAccount(event.paymentId(),
                "PROCESSOR_CLEARING"));
        assertEquals(1L, countEntriesForAccount(event.paymentId(),
                "MERCHANT_PAYABLE"));
    }

    @Test
    void repeatedPaymentEventDoesNotCreateDuplicateJournalEntries() {
        PaymentSucceededEvent event = successfulPayment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("250.00")
        );

        postingService.postSuccessfulPayment(event);
        postingService.postSuccessfulPayment(event);

        assertEquals(1L, countTransactions(event.paymentId()));
        assertEquals(2L, countEntries(event.paymentId()));
    }

    @Test
    void conflictingEventForExistingPaymentIsRejected() {
        UUID paymentId = UUID.randomUUID();
        UUID originalEventId = UUID.randomUUID();

        PaymentSucceededEvent original = successfulPayment(
                paymentId,
                originalEventId,
                new BigDecimal("250.00")
        );

        PaymentSucceededEvent conflicting = new PaymentSucceededEvent(
                UUID.randomUUID(),
                Instant.parse("2026-10-25T00:01:00Z"),
                paymentId,
                original.orderId(),
                original.merchantId(),
                original.customerId(),
                new BigDecimal("300.00")
        );

        postingService.postSuccessfulPayment(original);

        assertThrows(IllegalStateException.class,
                () -> postingService.postSuccessfulPayment(conflicting));

        assertEquals(1L, countTransactions(paymentId));
        assertEquals(2L, countEntries(paymentId));

        assertEquals(250.00,
                jdbcTemplate.queryForObject("""
                        SELECT total_amount
                        FROM ledger_transactions
                        WHERE payment_id = ?
                        """, BigDecimal.class, paymentId).doubleValue(),
                0.0001
        );
    }

    @Test
    void rollsBackTransactionAndFirstEntryWhenSecondEntryInsertFails() {
        PaymentSucceededEvent event = successfulPayment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("500.00")
        );

        jdbcTemplate.execute("""
                CREATE FUNCTION reject_merchant_payable_entry()
                RETURNS TRIGGER AS $$
                BEGIN
                    IF NEW.account_code = 'MERCHANT_PAYABLE' THEN
                        RAISE EXCEPTION 'Simulated ledger entry failure';
                    END IF;

                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """);

        jdbcTemplate.execute("""
                CREATE TRIGGER trg_reject_merchant_payable_entry
                BEFORE INSERT ON ledger_entries
                FOR EACH ROW
                EXECUTE FUNCTION reject_merchant_payable_entry()
                """);

        assertThrows(DataAccessException.class,
                () -> postingService.postSuccessfulPayment(event)
        );

        assertEquals(0L, countTransactions(event.paymentId()));
        assertEquals(0L, countEntries(event.paymentId()));
    }

    private PaymentSucceededEvent successfulPayment(
            UUID paymentId,
            UUID eventId,
            BigDecimal amount) {

        return new PaymentSucceededEvent(
                eventId,
                Instant.parse("2026-10-25T00:00:00Z"),
                paymentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                amount
        );
    }

    private long countTransactions(UUID paymentId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM ledger_transactions
                WHERE payment_id = ?
                """, Long.class, paymentId);
    }

    private long countEntries(UUID paymentId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM ledger_entries le
                JOIN ledger_transactions lt
                    ON lt.transaction_id = le.transaction_id
                WHERE lt.payment_id = ?
                """, Long.class, paymentId);
    }

    private BigDecimal sumAmount(UUID paymentId, String columnName) {
        if (!"debit_amount".equals(columnName)
                && !"credit_amount".equals(columnName)) {
            throw new IllegalArgumentException(
                    "Unsupported ledger amount column: " + columnName
            );
        }

        return jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(le.%s), 0)
                FROM ledger_entries le
                JOIN ledger_transactions lt
                    ON lt.transaction_id = le.transaction_id
                WHERE lt.payment_id = ?
                """.formatted(columnName),
                BigDecimal.class,
                paymentId
        );
    }

    private long countEntriesForAccount(
            UUID paymentId,
            String accountCode) {

        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM ledger_entries le
                JOIN ledger_transactions lt
                    ON lt.transaction_id = le.transaction_id
                WHERE lt.payment_id = ?
                  AND le.account_code = ?
                """, Long.class, paymentId, accountCode);
    }
}
