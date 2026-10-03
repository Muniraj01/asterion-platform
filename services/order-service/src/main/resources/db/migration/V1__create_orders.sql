CREATE TABLE orders (
                        order_id UUID PRIMARY KEY,
                        merchant_id UUID NOT NULL,
                        customer_id UUID NOT NULL,
                        total_amount NUMERIC(19, 4) NOT NULL,
                        status VARCHAR(32) NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                        CONSTRAINT chk_orders_total_amount_non_negative
                            CHECK (total_amount >= 0),

                        CONSTRAINT chk_orders_status
                            CHECK (status IN ('CREATED', 'CANCELLED', 'COMPLETED'))
);

CREATE INDEX idx_orders_customer_created_at
    ON orders (customer_id, created_at DESC);

CREATE INDEX idx_orders_merchant_created_at
    ON orders (merchant_id, created_at DESC);


CREATE TABLE order_outbox_events (
                                     event_id UUID PRIMARY KEY,
                                     aggregate_id UUID NOT NULL,
                                     event_type VARCHAR(255) NOT NULL,
                                     payload JSONB NOT NULL,
                                     created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                     status VARCHAR(32) NOT NULL,
                                     claimed_at TIMESTAMP WITH TIME ZONE NULL,
                                     published_at TIMESTAMP WITH TIME ZONE NULL
);

CREATE INDEX idx_order_outbox_pending
    ON order_outbox_events (status, created_at);

CREATE INDEX idx_order_outbox_claimed
    ON order_outbox_events (status, claimed_at);