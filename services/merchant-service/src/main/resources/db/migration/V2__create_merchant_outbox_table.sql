CREATE TABLE merchant_outbox (
                                 event_id UUID PRIMARY KEY,
                                 aggregate_id UUID NOT NULL,
                                 event_type VARCHAR(100) NOT NULL,
                                 payload JSONB NOT NULL,
                                 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                 published_at TIMESTAMP WITH TIME ZONE NULL,
                                 status VARCHAR(30) NOT NULL
);

CREATE INDEX idx_merchant_outbox_status_created_at
    ON merchant_outbox (status, created_at);