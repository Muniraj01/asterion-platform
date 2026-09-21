ALTER TABLE merchant_outbox
    ADD COLUMN claimed_at TIMESTAMP WITH TIME ZONE NULL;

CREATE INDEX idx_merchant_outbox_claimable
    ON merchant_outbox (status, claimed_at, created_at);
