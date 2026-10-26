
package com.asterion.ledger.application;

import com.asterion.events.PaymentSucceededEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class PaymentLedgerPostingService {

    private static final String PAYMENT_SUCCEEDED = "payment.succeeded.v1";
    private static final String PROCESSOR_CLEARING = "PROCESSOR_CLEARING";
    private static final String MERCHANT_PAYABLE = "MERCHANT_PAYABLE";

    private final JdbcTemplate jdbcTemplate;

    public PaymentLedgerPostingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Transactional
    public void postSuccessfulPayment(PaymentSucceededEvent event) {
        validate(event);

        BigDecimal amount = event.totalAmount()
                .setScale(4, RoundingMode.UNNECESSARY);

        UUID transactionId = UUID.randomUUID();

        int inserted = jdbcTemplate.update("""
                INSERT INTO ledger_transactions (
                    transaction_id,
                    payment_id,
                    event_id,
                    order_id,
                    merchant_id,
                    customer_id,
                    total_amount,
                    event_type
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (payment_id) DO NOTHING
                """,
                transactionId,
                event.paymentId(),
                event.eventId(),
                event.orderId(),
                event.merchantId(),
                event.customerId(),
                amount,
                PAYMENT_SUCCEEDED
        );

        if (inserted == 0) {
            verifyExistingTransaction(event, amount);
            return;
        }

        insertEntry(
                transactionId, PROCESSOR_CLEARING, amount, BigDecimal.ZERO.setScale(4));

        insertEntry(
                transactionId, MERCHANT_PAYABLE, BigDecimal.ZERO.setScale(4), amount);
    }

    private void insertEntry(
            UUID transactionId,
            String accountCode,
            BigDecimal debit,
            BigDecimal credit) {

        jdbcTemplate.update("""
                INSERT INTO ledger_entries (
                    entry_id,
                    transaction_id,
                    account_code,
                    debit_amount,
                    credit_amount
                )
                VALUES (?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                transactionId,
                accountCode,
                debit,
                credit
        );
    }

    private void verifyExistingTransaction(
            PaymentSucceededEvent event, BigDecimal amount) {
        Map<String, Object> existing = jdbcTemplate.queryForMap("""
                SELECT event_id,
                       order_id,
                       merchant_id,
                       customer_id,
                       total_amount,
                       event_type
                FROM ledger_transactions
                WHERE payment_id = ?
                """, event.paymentId());

        boolean matches =
                Objects.equals(existing.get("event_id"), event.eventId())
                        && Objects.equals(existing.get("order_id"), event.orderId())
                        && Objects.equals(existing.get("merchant_id"), event.merchantId())
                        && Objects.equals(existing.get("customer_id"), event.customerId())
                        && PAYMENT_SUCCEEDED.equals(existing.get("event_type"))
                        && sameAmount((BigDecimal) existing.get("total_amount"), amount
                );

        if (!matches) {
            throw new IllegalStateException(
                    "Conflicting ledger event for payment " + event.paymentId());
        }
    }

    private boolean sameAmount(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) == 0;
    }

    private void validate(PaymentSucceededEvent event) {
        if (event == null)
            throw new IllegalArgumentException("Payment event must not be null");

        if (event.eventId() == null
                || event.paymentId() == null
                || event.orderId() == null
                || event.merchantId() == null
                || event.customerId() == null) {
            throw new IllegalArgumentException(
                    "Payment event identifiers must not be null");
        }

        BigDecimal amount = event.totalAmount();
        if (amount == null || amount.signum() <= 0)
            throw new IllegalArgumentException("Payment amount must be positive");

        try {
            BigDecimal normalized = amount.setScale(4, RoundingMode.UNNECESSARY);
            if (normalized.precision() > 19)
                throw new IllegalArgumentException(
                        "Payment amount exceeds NUMERIC(19,4) precision");

        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "Payment amount cannot have more than four fractional digits", exception);
        }
    }
}
