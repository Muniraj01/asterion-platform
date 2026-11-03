ALTER TABLE payments
    ADD COLUMN risk_decision VARCHAR(16),
    ADD COLUMN risk_reason_code VARCHAR(100),
    ADD COLUMN risk_assessed_at TIMESTAMP WITH TIME ZONE;

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
                       'FAILED',
                       'RISK_REJECTED',
                       'RISK_REVIEW'
                )
            );

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_risk_decision
        CHECK (
            risk_decision IS NULL
                OR risk_decision IN ('APPROVE', 'REJECT', 'REVIEW')
            );

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_risk_assessment_fields
        CHECK (
            risk_decision IS NULL
                OR (
                risk_reason_code IS NOT NULL
                    AND length(trim(risk_reason_code)) > 0
                    AND risk_assessed_at IS NOT NULL
                )
            );

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_risk_status_consistency
        CHECK (
            (status <> 'RISK_REJECTED' OR risk_decision = 'REJECT')
                AND
            (status <> 'RISK_REVIEW' OR risk_decision = 'REVIEW')
            );