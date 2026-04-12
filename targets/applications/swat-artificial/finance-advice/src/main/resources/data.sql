-- === ACCOUNTS ===
DELETE
FROM linked_institutions;
DELETE
FROM transactions;
DELETE
FROM rules;
DELETE
FROM rulesets;
DELETE
FROM accounts;

INSERT INTO accounts(id, user_id, account_type, currency, metadata)
VALUES ('acc-1', 'u1', 'CHECKING', 'EUR', 'primary household account'),
       ('acc-2', 'u2', 'SAVINGS', 'EUR', 'rainy day fund'),
       ('acc-3', 'u3', 'CHECKING', 'USD', 'freelancer business account'),
       ('acc-4', 'u4', 'INVESTMENT', 'EUR', 'long term ETF portfolio'),
       ('acc-5', 'u5', 'CHECKING', 'GBP', 'UK based salary account'),
       ('acc-6', 'u6', 'SAVINGS', 'USD', 'emergency reserve account');

-- === LINKED INSTITUTIONS ===
INSERT INTO linked_institutions(account_id, name)
VALUES ('acc-1', 'Sparkasse Lübeck'),
       ('acc-1', 'DKB'),
       ('acc-2', 'ING Diba'),
       ('acc-3', 'Chase Bank'),
       ('acc-3', 'Revolut'),
       ('acc-4', 'Comdirect'),
       ('acc-4', 'Scalable Capital'),
       ('acc-5', 'HSBC UK'),
       ('acc-5', 'Revolut'),
       ('acc-6', 'Wells Fargo'),
       ('acc-6', 'N26');

-- === TRANSACTIONS ===
INSERT INTO transactions(id, account_id, date_ms, amount_cents, currency, merchant, category, raw_text)
VALUES
    -- acc-1: German household
    ('tx-1', 'acc-1',
     DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', DATEADD('DAY', -12, CURRENT_TIMESTAMP)),
     -750000, 'EUR', 'Landlord', 'Rent', 'Monthly rent'),

    ('tx-2', 'acc-1',
     DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', DATEADD('DAY', -11, CURRENT_TIMESTAMP)),
     -12000, 'EUR', 'EDEKA', 'Food', 'Groceries'),

    ('tx-3', 'acc-1',
     DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', DATEADD('DAY', -10, CURRENT_TIMESTAMP)),
     -5400, 'EUR', 'Baker', 'Food', 'Bread and pastries'),

    ('tx-4', 'acc-1',
     DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', DATEADD('DAY', -7, CURRENT_TIMESTAMP)),
     -18000, 'EUR', 'Tankstelle', 'Transport', 'Fuel'),

    ('tx-5', 'acc-1',
     DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', DATEADD('DAY', -2, CURRENT_TIMESTAMP)),
     250000, 'EUR', 'Employer GmbH', 'Salary', 'Payroll 2024-02'),

    -- acc-2: Savings
    ('tx-6', 'acc-2',
     DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', DATEADD('DAY', -30, CURRENT_TIMESTAMP)),
     100000, 'EUR', 'Transfer from acc-1', 'Transfer', 'Manual transfer'),

    ('tx-7', 'acc-2',
     DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', DATEADD('DAY', -5, CURRENT_TIMESTAMP)),
     500, 'EUR', 'Interest', 'Interest', 'Monthly interest payout'),

    -- acc-3: USD freelancer
    ('tx-8', 'acc-3',
     DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', DATEADD('DAY', -15, CURRENT_TIMESTAMP)),
     1500000, 'USD', 'Upwork', 'Income', 'Invoice payment'),

    ('tx-9', 'acc-3',
     DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', DATEADD('DAY', -13, CURRENT_TIMESTAMP)),
     -45000, 'USD', 'GitHub', 'Subscription', 'Private repo plan'),

    ('tx-10', 'acc-3',
     DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', DATEADD('DAY', -3, CURRENT_TIMESTAMP)),
     -7500, 'USD', 'Starbucks', 'Food', 'Coffee & snacks'),

    -- acc-4: Investments
    ('tx-11', 'acc-4',
     DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', DATEADD('DAY', -40, CURRENT_TIMESTAMP)),
     -500000, 'EUR', 'ETF Purchase', 'Investment', 'SP500 ETF buy'),

    ('tx-12', 'acc-4',
     DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', DATEADD('DAY', -10, CURRENT_TIMESTAMP)),
     10000, 'EUR', 'Dividends AG', 'Investment', 'Quarterly dividend'),

    -- acc-5: UK account
    ('tx-13', 'acc-5',
     DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', DATEADD('DAY', -25, CURRENT_TIMESTAMP)),
     220000, 'GBP', 'TechCorp Ltd', 'Salary', 'Monthly pay'),

    ('tx-14', 'acc-5',
     DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', DATEADD('DAY', -23, CURRENT_TIMESTAMP)),
     -20000, 'GBP', 'Tesco', 'Food', 'Groceries'),

    ('tx-15', 'acc-5',
     DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', DATEADD('DAY', -20, CURRENT_TIMESTAMP)),
     -15000, 'GBP', 'Transport for London', 'Transport', 'Oyster top-up'),

    ('tx-16', 'acc-5',
     DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', DATEADD('DAY', -19, CURRENT_TIMESTAMP)),
     -5000, 'GBP', 'Costa Coffee', 'Food', 'Flat white'),

    -- acc-6: USD savings
    ('tx-17', 'acc-6',
     DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', DATEADD('DAY', -90, CURRENT_TIMESTAMP)),
     1000000, 'USD', 'Initial deposit', 'Transfer', 'Setup'),

    ('tx-18', 'acc-6',
     DATEDIFF('MILLISECOND', TIMESTAMP '1970-01-01 00:00:00', DATEADD('DAY', -60, CURRENT_TIMESTAMP)),
     2000, 'USD', 'Interest', 'Interest', 'Quarterly interest');

-- === RULESETS ===
INSERT INTO rulesets(id)
VALUES ('rs-food'),
       ('rs-leisure'),
       ('rs-large');

-- === RULES ===
INSERT INTO rules(id, ruleset_id, expr)
VALUES ('r1', 'rs-food', 'category == ''Food'' && amount > 1000'),
       ('r2', 'rs-food', 'merchant ~= ''.*(EDEKA|SuperMart).*'''),
       ('r3', 'rs-food', 'raw_text ~= ''grocer.*'''),
       ('r4', 'rs-leisure', 'category == ''Leisure'' && amount > 5000'),
       ('r5', 'rs-leisure', 'merchant ~= ''(Cinema|Netflix|Spotify)'''),
       ('r6', 'rs-large', 'amount > 100000 && category != ''Salary'''),
       ('r7', 'rs-large', 'amount > 500000 && merchant != ''Employer GmbH'''),
       ('r8', 'rs-large', 'category == ''Investment'' && amount > 100000');