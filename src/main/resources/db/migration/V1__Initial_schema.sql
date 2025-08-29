CREATE TABLE wallet (
            id BIGSERIAL PRIMARY KEY,
            customer_id VARCHAR(255) UNIQUE NOT NULL,
            balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,
            version BIGINT NOT NULL DEFAULT 0,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE transaction_ledger (
             id BIGSERIAL PRIMARY KEY,
             transaction_id VARCHAR(255) UNIQUE NOT NULL,
             request_id VARCHAR(255) UNIQUE,
             wallet_id BIGINT NOT NULL REFERENCES wallet(id),
             type VARCHAR(20) NOT NULL CHECK (type IN ('TOPUP', 'CONSUME')),
             amount DECIMAL(19,2) NOT NULL,
             description TEXT,
             status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reconciliation_records (
           id BIGSERIAL PRIMARY KEY,
           reconciliation_date DATE NOT NULL,
           reconciliation_id VARCHAR(255) UNIQUE NOT NULL,
           internal_transaction_id VARCHAR(255),
           external_transaction_id VARCHAR(255),
           internal_amount DECIMAL(19,2),
           external_amount DECIMAL(19,2),
           discrepancy_amount DECIMAL(19,2),
           discrepancy_reason TEXT,
           status VARCHAR(20) NOT NULL CHECK (status IN ('MATCHED', 'MISSING_INTERNAL', 'MISSING_EXTERNAL', 'AMOUNT_MISMATCH')),
           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reconciliation_date ON reconciliation_records(reconciliation_date);
CREATE INDEX idx_reconciliation_status ON reconciliation_records(status);

CREATE INDEX idx_transaction_ledger_wallet_id ON transaction_ledger(wallet_id);
CREATE INDEX idx_transaction_ledger_transaction_id ON transaction_ledger(transaction_id);
CREATE INDEX idx_transaction_ledger_created_at ON transaction_ledger(created_at);
CREATE INDEX idx_wallet_customer_id ON wallet(customer_id);