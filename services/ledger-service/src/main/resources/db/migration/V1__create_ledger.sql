CREATE TABLE ledger_transactions (
    transaction_id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    event_id UUID NOT NULL,
    order_id UUID NOT NULL,
    merchant_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    total_amount NUMERIC(19, 4) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_ledger_transaction_payment UNIQUE (payment_id),
    CONSTRAINT uq_ledger_transaction_event UNIQUE (event_id),
    CONSTRAINT chk_ledger_transaction_amount CHECK (total_amount > 0)
);

CREATE TABLE ledger_entries (
    entry_id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    account_code VARCHAR(64) NOT NULL,
    debit_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    credit_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ledger_entry_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES ledger_transactions (transaction_id),

    CONSTRAINT uq_ledger_entry_transaction_account
        UNIQUE (transaction_id, account_code),

    CONSTRAINT chk_ledger_entry_one_sided
        CHECK (
            (debit_amount > 0 AND credit_amount = 0)
            OR
            (credit_amount > 0 AND debit_amount = 0)
        )
);

CREATE INDEX idx_ledger_transactions_merchant_created
    ON ledger_transactions (merchant_id, created_at DESC);

CREATE INDEX idx_ledger_transactions_order
    ON ledger_transactions (order_id);

CREATE INDEX idx_ledger_entries_transaction
    ON ledger_entries (transaction_id);
