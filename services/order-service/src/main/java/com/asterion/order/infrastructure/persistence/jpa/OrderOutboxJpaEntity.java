package com.asterion.order.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_outbox_events")
public class OrderOutboxJpaEntity {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OrderOutboxJpaEntity() {
    }

    public OrderOutboxJpaEntity(
            UUID eventId,
            UUID aggregateId,
            String eventType,
            String payload,
            Instant createdAt,
            String status,
            Instant claimedAt) {

        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = createdAt;
        this.status = status;
        this.claimedAt = claimedAt;
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

    public String getStatus() {
        return status;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void markClaimed(Instant claimedAt) {
        this.claimedAt = claimedAt;
        this.status = "CLAIMED";
    }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt;
        this.claimedAt = null;
        this.status = "PUBLISHED";
    }

    public void releaseClaim() {
        this.claimedAt = null;
        this.status = "NEW";
    }
}