DROP TABLE IF EXISTS linked_institutions;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS rules;
DROP TABLE IF EXISTS rulesets;
DROP TABLE IF EXISTS accounts;

CREATE TABLE accounts (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    account_type VARCHAR(32) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    metadata VARCHAR(1024)
);

CREATE TABLE linked_institutions (
    account_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (account_id, name),
    CONSTRAINT fk_li_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

CREATE TABLE transactions (
    id VARCHAR(64) PRIMARY KEY,
    account_id VARCHAR(64) NOT NULL,
    date_ms BIGINT NOT NULL,
    amount_cents BIGINT,
    currency VARCHAR(8),
    merchant VARCHAR(255),
    category VARCHAR(64),
    raw_text VARCHAR(1024),
    CONSTRAINT fk_tx_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

CREATE TABLE rulesets (
    id VARCHAR(64) PRIMARY KEY
);

CREATE TABLE rules (
    id VARCHAR(64) PRIMARY KEY,
    ruleset_id VARCHAR(64) NOT NULL,
    expr VARCHAR(1024) NOT NULL,
    CONSTRAINT fk_rule_set FOREIGN KEY (ruleset_id) REFERENCES rulesets(id) ON DELETE CASCADE
);