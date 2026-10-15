
package com.asterion.payment.application.port.out;

import com.asterion.payment.application.model.PaymentOutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentOutboxRepository {

    List<PaymentOutboxEvent> claimPending(
            int limit, Instant now, Instant staleBefore);

    void markPublished(UUID eventId);
}
