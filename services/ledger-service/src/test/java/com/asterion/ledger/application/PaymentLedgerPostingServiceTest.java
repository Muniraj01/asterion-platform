
package com.asterion.ledger.application;

import com.asterion.events.PaymentSucceededEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentLedgerPostingServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PaymentLedgerPostingService service;

    private UUID eventId;
    private UUID paymentId;
    private UUID orderId;
    private UUID merchantId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        service = new PaymentLedgerPostingService(jdbcTemplate);

        eventId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        merchantId = UUID.randomUUID();
        customerId = UUID.randomUUID();
    }

    @Test
    void postsBalancedEntriesForSuccessfulPayment() {
        PaymentSucceededEvent event = successfulPayment(
                new BigDecimal("1000.0000"));

        when(jdbcTemplate.update(anyString(), any(Object[].class)))
                .thenReturn(1, 1, 1);

        assertDoesNotThrow(() -> service.postSuccessfulPayment(event));

        // One transaction record and two journal entries.
        verify(jdbcTemplate, times(3))
                .update(anyString(), any(Object[].class));
    }

    @Test
    void doesNotCreateDuplicateEntriesForRepeatedPaymentEvent() {
        PaymentSucceededEvent event = successfulPayment(
                new BigDecimal("1000.00"));

        when(jdbcTemplate.update(anyString(), any(Object[].class)))
                .thenReturn(0);

        when(jdbcTemplate.queryForMap(anyString(), any(Object[].class)))
                .thenReturn(existingTransaction(event));

        assertDoesNotThrow(() -> service.postSuccessfulPayment(event));

        // Only the attempted transaction insert occurs.
        verify(jdbcTemplate, times(1))
                .update(anyString(), any(Object[].class));
    }

    @Test
    void rejectsConflictingEventForExistingPayment() {
        PaymentSucceededEvent original = successfulPayment(
                new BigDecimal("1000.00"));

        PaymentSucceededEvent conflicting = new PaymentSucceededEvent(
                eventId,
                Instant.now(),
                paymentId,
                orderId,
                merchantId,
                customerId,
                new BigDecimal("1200.00")
        );

        when(jdbcTemplate.update(anyString(), any(Object[].class)))
                .thenReturn(0);

        when(jdbcTemplate.queryForMap(anyString(), any(Object[].class)))
                .thenReturn(existingTransaction(original));

        assertThrows(IllegalStateException.class,
                () -> service.postSuccessfulPayment(conflicting));

        verify(jdbcTemplate, times(1))
                .update(anyString(), any(Object[].class));
    }

    @Test
    void rejectsZeroAmount() {
        PaymentSucceededEvent event = successfulPayment(BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class,
                () -> service.postSuccessfulPayment(event));
    }

    @Test
    void rejectsNegativeAmount() {
        PaymentSucceededEvent event = successfulPayment(
                new BigDecimal("-10.00"));

        assertThrows(IllegalArgumentException.class,
                () -> service.postSuccessfulPayment(event)
        );
    }

    @Test
    void rejectsAmountWithMoreThanFourSignificantFractionalDigits() {
        PaymentSucceededEvent event = successfulPayment(
                new BigDecimal("10.12345"));

        assertThrows(IllegalArgumentException.class,
                () -> service.postSuccessfulPayment(event)
        );
    }

    private PaymentSucceededEvent successfulPayment(BigDecimal amount) {
        return new PaymentSucceededEvent(
                eventId,
                Instant.parse("2026-10-25T00:00:00Z"),
                paymentId,
                orderId,
                merchantId,
                customerId,
                amount
        );
    }

    private Map<String, Object> existingTransaction(
            PaymentSucceededEvent event) {

        return Map.of(
                "event_id", event.eventId(),
                "order_id", event.orderId(),
                "merchant_id", event.merchantId(),
                "customer_id", event.customerId(),
                "total_amount", event.totalAmount(),
                "event_type", "payment.succeeded.v1"
        );
    }
}
