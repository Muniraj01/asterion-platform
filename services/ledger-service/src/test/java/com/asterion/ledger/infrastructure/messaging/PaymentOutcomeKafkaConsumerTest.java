
package com.asterion.ledger.infrastructure.messaging;

import com.asterion.events.PaymentFailedEvent;
import com.asterion.events.PaymentSucceededEvent;
import com.asterion.ledger.application.PaymentLedgerPostingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PaymentOutcomeKafkaConsumerTest {

    private static final String TOPIC = "payment.events";
    private static final String EVENT_TYPE_HEADER = "eventType";
    private static final String PAYMENT_SUCCEEDED = "payment.succeeded.v1";
    private static final String PAYMENT_FAILED = "payment.failed.v1";

    @Mock
    private PaymentLedgerPostingService postingService;

    private ObjectMapper objectMapper;
    private PaymentOutcomeKafkaConsumer consumer;

    private UUID eventId;
    private UUID paymentId;
    private UUID orderId;
    private UUID merchantId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        consumer = new PaymentOutcomeKafkaConsumer(objectMapper, postingService);

        eventId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        merchantId = UUID.randomUUID();
        customerId = UUID.randomUUID();
    }

    @Test
    void successfulPaymentIsForwardedToLedgerPostingService() throws Exception {
        PaymentSucceededEvent event = successfulPayment();
        ConsumerRecord<String, String> record = record(
                PAYMENT_SUCCEEDED, objectMapper.writeValueAsString(event));

        assertDoesNotThrow(() -> consumer.consume(record));

        verify(postingService).postSuccessfulPayment(any(PaymentSucceededEvent.class));
    }

    @Test
    void failedPaymentDoesNotCreateSuccessfulPaymentPosting() throws Exception {
        PaymentFailedEvent event = failedPayment();
        ConsumerRecord<String, String> record = record(
                PAYMENT_FAILED, objectMapper.writeValueAsString(event));

        assertDoesNotThrow(() -> consumer.consume(record));

        verifyNoInteractions(postingService);
    }

    @Test
    void rejectsRecordWithMissingEventTypeHeader() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                TOPIC,
                0,
                0L,
                paymentId.toString(),
                "{}"
        );

        assertThrows(PermanentLedgerEventException.class,
                () -> consumer.consume(record));

        verifyNoInteractions(postingService);
    }

    @Test
    void rejectsUnsupportedEventType() {
        ConsumerRecord<String, String> record = record(
                "payment.refunded.v1", "{}");

        assertThrows(PermanentLedgerEventException.class,
                () -> consumer.consume(record));

        verifyNoInteractions(postingService);
    }

    @Test
    void rejectsMalformedSuccessfulPaymentPayload() {
        ConsumerRecord<String, String> record = record(
                PAYMENT_SUCCEEDED, "{not-valid-json");

        assertThrows(PermanentLedgerEventException.class,
                () -> consumer.consume(record));

        verifyNoInteractions(postingService);
    }

    @Test
    void rejectsBlankPayload() {
        ConsumerRecord<String, String> record = record(
                PAYMENT_SUCCEEDED, "   ");

        assertThrows(PermanentLedgerEventException.class,
                () -> consumer.consume(record));

        verifyNoInteractions(postingService);
    }

    private ConsumerRecord<String, String> record(
            String eventType,
            String payload) {

        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                TOPIC,
                0,
                0L,
                paymentId.toString(),
                payload
        );

        record.headers().add(new RecordHeader(
                EVENT_TYPE_HEADER, eventType.getBytes(StandardCharsets.UTF_8)));

        return record;
    }

    private PaymentSucceededEvent successfulPayment() {
        return new PaymentSucceededEvent(
                eventId,
                Instant.parse("2026-10-25T00:00:00Z"),
                paymentId,
                orderId,
                merchantId,
                customerId,
                new BigDecimal("1000.00")
        );
    }

    private PaymentFailedEvent failedPayment() {
        return new PaymentFailedEvent(
                eventId,
                Instant.parse("2026-10-25T00:00:00Z"),
                paymentId,
                orderId,
                merchantId,
                customerId,
                new BigDecimal("1000.00")
        );
    }
}
