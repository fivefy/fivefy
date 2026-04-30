-- 없는 테이블에 새로 생성
-- billing_keys: billing_key 컬럼 생성
CREATE TABLE billing_keys
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    created_at  datetime NULL,
    user_id     BIGINT       NOT NULL,
    billing_key VARCHAR(255) NOT NULL,
    card_last4  VARCHAR(10)  NULL,
    card_name   VARCHAR(50)  NULL,
    pay_method  VARCHAR(50)  NULL,
    active      BIT(1)       NOT NULL,
    CONSTRAINT pk_billing_keys PRIMARY KEY (id),
    CONSTRAINT uc_billing_keys_billingkey UNIQUE (billing_key)
);

-- 이미 있는 테이블에 내용 추가
-- point_histories: 컬럼 추가
ALTER TABLE point_histories
    ADD COLUMN cash_order_id  BIGINT NULL,
    ADD COLUMN point_order_id BIGINT NULL;

-- point_histories: 복합 unique 추가
ALTER TABLE point_histories
    ADD CONSTRAINT uc_point_histories_cash_order_history
        UNIQUE (cash_order_id, point_history_type);

ALTER TABLE point_histories
    ADD CONSTRAINT uc_point_histories_point_order_history
        UNIQUE (point_order_id, point_history_type);