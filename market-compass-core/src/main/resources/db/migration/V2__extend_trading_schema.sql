ALTER TABLE holdings
    ADD COLUMN name VARCHAR(200),
    ADD COLUMN asset_type VARCHAR(10) NOT NULL DEFAULT 'STOCK' CHECK (asset_type IN ('STOCK', 'ETF')),
    ADD COLUMN sector VARCHAR(100),
    ADD COLUMN current_price NUMERIC(19,6) NOT NULL DEFAULT 0 CHECK (current_price >= 0),
    ADD COLUMN annual_dividend_per_share NUMERIC(19,6) NOT NULL DEFAULT 0 CHECK (annual_dividend_per_share >= 0);

ALTER TABLE trades
    ADD COLUMN asset_name VARCHAR(200),
    ADD COLUMN asset_type VARCHAR(10) NOT NULL DEFAULT 'STOCK' CHECK (asset_type IN ('STOCK', 'ETF')),
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN gain_loss NUMERIC(19,6) NOT NULL DEFAULT 0,
    ADD COLUMN balance_after NUMERIC(19,6);

CREATE TABLE paper_wallets (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL UNIQUE REFERENCES portfolios(id) ON DELETE CASCADE,
    starting_balance NUMERIC(19,6) NOT NULL CHECK (starting_balance > 0),
    available_balance NUMERIC(19,6) NOT NULL CHECK (available_balance >= 0),
    realized_gain_loss NUMERIC(19,6) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_holdings_portfolio_ticker ON holdings(portfolio_id, ticker);
CREATE INDEX idx_trades_portfolio_ticker ON trades(portfolio_id, ticker);
