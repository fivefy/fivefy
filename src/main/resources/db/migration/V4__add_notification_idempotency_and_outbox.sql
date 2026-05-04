-- notifications 테이블에 idempotency key 및 관련 컬럼 추가
ALTER TABLE notifications
    ADD COLUMN actor_id        BIGINT       NULL,
    ADD COLUMN resource_id     BIGINT       NULL,
    ADD COLUMN idempotency_key VARCHAR(255) NULL;

ALTER TABLE notifications
    ADD CONSTRAINT uc_notifications_idempotency_key UNIQUE (idempotency_key);

-- notification_outbox 테이블 생성
CREATE TABLE notification_outbox
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    event_type     VARCHAR(50)  NOT NULL,
    target_user_id BIGINT       NOT NULL,
    actor_id       BIGINT       NULL,
    resource_id    BIGINT       NULL,
    content        VARCHAR(500) NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at     DATETIME     NOT NULL,
    processed_at   DATETIME     NULL,
    CONSTRAINT pk_notification_outbox PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_status_created ON notification_outbox (status, created_at);