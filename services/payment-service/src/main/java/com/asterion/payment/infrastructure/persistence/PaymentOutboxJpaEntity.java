
package com.asterion.payment.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_outbox_events")
public class PaymentOutboxJpaEntity {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    protected PaymentOutboxJpaEntity() {
    }

    public PaymentOutboxJpaEntity(
            UUID eventId,
            UUID aggregateId,
            String eventType,
            String payload,
            Instant createdAt) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = createdAt;
        this.status = "NEW";
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getStatus() {
        return status;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public void markClaimed(Instant now) {
        this.status = "PROCESSING";
        this.claimedAt = now;
    }

    public void markPublished(Instant now) {
        this.status = "PUBLISHED";
        this.publishedAt = now;
        this.claimedAt = null;
    }
}
