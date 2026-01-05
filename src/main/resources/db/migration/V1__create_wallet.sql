CREATE TABLE wallets (
    user_id BIGINT PRIMARY KEY,
    balance BIGINT NOT NULL,
    version BIGINT NOT NULL
);

INSERT INTO wallets (user_id, balance, version)
VALUES (1, 100000, 0);
