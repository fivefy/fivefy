CREATE TABLE billing_attempts
(
    id              BIGINT AUTO_INCREMENT NOT NULL,
    created_at      datetime     NULL,
    user_id         BIGINT       NOT NULL,
    billing_key_id  BIGINT       NOT NULL,
    billing_cycle   VARCHAR(7)   NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    failure_reason  VARCHAR(50)  NULL,
    attempted_at    datetime     NOT NULL,
    CONSTRAINT pk_billing_attempts PRIMARY KEY (id)
);

CREATE INDEX idx_billing_attempts_user_id
    ON billing_attempts (user_id);

CREATE INDEX idx_billing_attempts_billing_key_id
    ON billing_attempts (billing_key_id);