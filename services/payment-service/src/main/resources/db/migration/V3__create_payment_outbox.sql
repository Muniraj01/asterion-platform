
CREATE TABLE payment_outbox_events
(
    event_id     UUID PRIMARY KEY,
    aggregate_id UUID         NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      JSONB        NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    published_at TIMESTAMPTZ,
    status       VARCHAR(30)  NOT NULL DEFAULT 'NEW',
    claimed_at   TIMESTAMPTZ,

    CONSTRAINT chk_payment_outbox_status
        CHECK (status IN ('NEW', 'PROCESSING', 'PUBLISHED'))
);

CREATE INDEX idx_payment_outbox_pending
    ON payment_outbox_events (created_at)
    WHERE status = 'NEW';

CREATE INDEX idx_payment_outbox_stale_claims
    ON payment_outbox_events (claimed_at)
    WHERE status = 'PROCESSING';
