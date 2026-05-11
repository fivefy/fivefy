-- webhook_events: 포트원 웹훅 중복 수신 방지용 이벤트 저장 테이블
-- webhook_event_id: 포트원이 부여하는 웹훅 고유 ID
-- payment_id: 결제 건 식별자 (포트원 pg_transaction_id와 대응)
-- received_at: 웹훅 수신 시각
CREATE TABLE webhook_events
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    created_at       datetime              NULL,
    webhook_event_id VARCHAR(255)          NOT NULL,
    payment_id       VARCHAR(255)          NOT NULL,
    received_at      datetime              NOT NULL,
    CONSTRAINT pk_webhook_events PRIMARY KEY (id)
);

-- webhook_event_id 단독 unique: 동일 웹훅 ID 재수신 차단
ALTER TABLE webhook_events
    ADD CONSTRAINT uc_webhook_events_webhook_event_id
        UNIQUE (webhook_event_id);

-- payment_id 단독 unique: 동일 결제 건에 대한 웹훅 중복 처리 차단
ALTER TABLE webhook_events
    ADD CONSTRAINT uc_webhook_events_payment_id
        UNIQUE (payment_id);

-- 조회 성능용 인덱스 (payment_id로 이미 처리됐는지 조회 시)
CREATE INDEX idx_webhook_events_payment_id
    ON webhook_events (payment_id);