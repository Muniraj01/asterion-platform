CREATE TABLE payments (
                          payment_id UUID PRIMARY KEY,
                          order_id UUID NOT NULL,
                          merchant_id UUID NOT NULL,
                          customer_id UUID NOT NULL,
                          source_event_id UUID NOT NULL,
                          total_amount NUMERIC(19, 4) NOT NULL,
                          status VARCHAR(32) NOT NULL,
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                          CONSTRAINT uq_payments_order_id
                              UNIQUE (order_id),

                          CONSTRAINT uq_payments_source_event_id
                              UNIQUE (source_event_id),

                          CONSTRAINT chk_payments_total_amount_non_negative
                              CHECK (total_amount >= 0),

                          CONSTRAINT chk_payments_status
                              CHECK (status IN (
                                                'INITIATED',
                                                'PROCESSING',
                                                'SUCCEEDED',
                                                'FAILED'
                                  ))
);

CREATE INDEX idx_payments_customer_created_at
    ON payments (customer_id, created_at DESC);

CREATE INDEX idx_payments_merchant_created_at
    ON payments (merchant_id, created_at DESC);