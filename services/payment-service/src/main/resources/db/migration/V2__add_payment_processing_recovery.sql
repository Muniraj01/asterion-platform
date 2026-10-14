ALTER TABLE payments
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_attempt_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN processing_lease_until TIMESTAMP WITH TIME ZONE;

ALTER TABLE payments
DROP CONSTRAINT chk_payments_status;

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_status
        CHECK (
            status IN (
                       'INITIATED',
                       'PROCESSING',
                       'RETRYABLE',
                       'SUCCEEDED',
                       'FAILED'
                )
            );

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_attempt_count
        CHECK (attempt_count >= 0);

CREATE INDEX idx_payments_processing_recovery
    ON payments (status, next_attempt_at, processing_lease_until);