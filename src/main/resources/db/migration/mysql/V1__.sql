-- =============================================================================
-- Fivefy MySQL Baseline (V1 통합본)
-- =============================================================================
-- 기존 V1 ~ V11 의 모든 변경 사항을 단일 baseline 으로 통합한 마이그레이션.
--
-- 통합 적용 사항 요약:
--   V1  : 초기 테이블 / 인덱스 / UNIQUE 생성
--   V2  : billing_keys 신규, point_histories 컬럼·UNIQUE 추가
--   V3  : tracks / track_comments 성능 인덱스
--   V4  : billing_attempts 신규
--   V5  : chat_* 신규, playlists.deleted, popular_charts UNIQUE, 컬럼 타입 조정
--   V6  : notifications idempotency, notification_outbox 신규
--   V7  : album/artist_applications generated column + UNIQUE (중복 방지 2차 방어선)
--   V8  : *_application_id UNIQUE, track_applications generated columns + UNIQUE (다층 방어선 최종)
--   V9  : webhook_events 신규 (PortOne 웹훅 중복 수신 방지)
--   V10 : subscriptions.user_id 컬럼 제거
--   V11 : track_applications/tracks audio_url → audio_key 컬럼명 변경
--         (V8 free_creation_pending_key 는 audio_url 참조 제거 후 title 기반으로 재생성됨)
-- =============================================================================


-- =============================================================================
-- 사용자 / 인증
-- =============================================================================

CREATE TABLE users
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    created_at     datetime NULL,
    email          VARCHAR(255) NOT NULL,
    password       VARCHAR(255) NOT NULL,
    name           VARCHAR(255) NOT NULL,
    `role`         VARCHAR(255) NOT NULL,
    status         VARCHAR(255) NOT NULL,
    last_active_at datetime NULL,
    updated_at     datetime NULL,
    deleted_at     datetime NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);


-- =============================================================================
-- 아티스트 도메인
-- =============================================================================

CREATE TABLE artists
(
    id                    BIGINT AUTO_INCREMENT NOT NULL,
    created_at            datetime NULL,
    owner_user_id         BIGINT       NOT NULL,
    name                  VARCHAR(100) NOT NULL,
    artist_type           VARCHAR(20)  NOT NULL,
    bio                   VARCHAR(1000) NULL,                       -- V5: TEXT → VARCHAR(1000)
    profile_image_url     VARCHAR(256) NULL,                        -- V5: VARCHAR(255) → 256
    status                VARCHAR(20)  NOT NULL,
    artist_application_id BIGINT NULL,                              -- V8: 승인 결과 엔티티 중복 방지
    updated_at            datetime     NOT NULL,
    deleted_at            datetime NULL,
    CONSTRAINT pk_artists PRIMARY KEY (id)
);

CREATE TABLE artist_applications
(
    id                   BIGINT AUTO_INCREMENT NOT NULL,
    created_at           datetime NULL,
    requester_user_id    BIGINT       NOT NULL,
    requested_name       VARCHAR(100) NOT NULL,
    artist_type          VARCHAR(20)  NOT NULL,
    bio                  TEXT NULL,
    profile_image_url    VARCHAR(255) NULL,
    status               VARCHAR(20)  NOT NULL,
    reviewed_by_admin_id BIGINT NULL,
    reviewed_at          datetime NULL,
    rejection_reason     VARCHAR(255) NULL,
    updated_at           datetime     NOT NULL,
    -- V7: 신청 row 중복 생성 방지 (TOCTOU 2차 방어선)
    active_duplicate_key VARCHAR(500)
        GENERATED ALWAYS AS (
            CASE
                WHEN status IN ('PENDING', 'APPROVED')
                    THEN CONCAT(requester_user_id, ':', requested_name, ':', artist_type)
                ELSE NULL
                END
            ) STORED,
    CONSTRAINT pk_artist_applications PRIMARY KEY (id)
);


-- =============================================================================
-- 앨범 도메인
-- =============================================================================

CREATE TABLE albums
(
    id                   BIGINT AUTO_INCREMENT NOT NULL,
    created_at           datetime NULL,
    artist_id            BIGINT       NOT NULL,
    title                VARCHAR(150) NOT NULL,
    `description`        TEXT NULL,
    cover_image_url      VARCHAR(255) NULL,
    status               VARCHAR(20)  NOT NULL,
    scheduled_publish_at datetime NULL,
    published_at         datetime NULL,
    track_count          BIGINT       NOT NULL,
    total_duration_sec   BIGINT       NOT NULL,
    album_application_id BIGINT NULL,                               -- V8: 승인 결과 엔티티 중복 방지
    updated_at           datetime     NOT NULL,
    deleted_at           datetime NULL,
    CONSTRAINT pk_albums PRIMARY KEY (id)
);

CREATE TABLE album_applications
(
    id                   BIGINT AUTO_INCREMENT NOT NULL,
    created_at           datetime NULL,
    requester_user_id    BIGINT       NOT NULL,
    artist_id            BIGINT       NOT NULL,
    title                VARCHAR(150) NOT NULL,
    `description`        TEXT NULL,
    cover_image_url      VARCHAR(255) NULL,
    publish_delay_days   INT          NOT NULL,
    status               VARCHAR(20)  NOT NULL,
    reviewed_by_admin_id BIGINT NULL,
    reviewed_at          datetime NULL,
    rejection_reason     VARCHAR(255) NULL,
    updated_at           datetime     NOT NULL,
    -- V7: 신청 row 중복 생성 방지 (TOCTOU 2차 방어선)
    active_duplicate_key VARCHAR(500)
        GENERATED ALWAYS AS (
            CASE
                WHEN status IN ('PENDING', 'APPROVED')
                    THEN CONCAT(requester_user_id, ':', artist_id, ':', title)
                ELSE NULL
                END
            ) STORED,
    CONSTRAINT pk_album_applications PRIMARY KEY (id)
);


-- =============================================================================
-- 트랙 도메인
-- =============================================================================

CREATE TABLE tracks
(
    id                   BIGINT AUTO_INCREMENT NOT NULL,
    created_at           datetime NULL,
    owner_user_id        BIGINT       NOT NULL,
    track_type           VARCHAR(30)  NOT NULL,
    artist_id            BIGINT NULL,
    album_id             BIGINT NULL,
    track_number         BIGINT NULL,
    title                VARCHAR(150) NOT NULL,
    lyrics               TEXT NULL,
    genre                VARCHAR(100) NOT NULL,
    audio_key            VARCHAR(255) NOT NULL,                     -- V11: audio_url → audio_key (Object Storage key)
    duration_sec         BIGINT       NOT NULL,
    featured_artist_text VARCHAR(255) NULL,
    status               VARCHAR(20)  NOT NULL,
    scheduled_publish_at datetime NULL,
    published_at         datetime NULL,
    play_count           BIGINT       NOT NULL,
    track_application_id BIGINT NULL,                               -- V8: 승인 결과 엔티티 중복 방지
    updated_at           datetime     NOT NULL,
    deleted_at           datetime NULL,
    CONSTRAINT pk_tracks PRIMARY KEY (id)
);

CREATE TABLE track_applications
(
    id                                BIGINT AUTO_INCREMENT NOT NULL,
    created_at                        datetime NULL,
    requester_user_id                 BIGINT       NOT NULL,
    track_type                        VARCHAR(30)  NOT NULL,
    artist_id                         BIGINT NULL,
    album_id                          BIGINT NULL,
    track_number                      BIGINT NULL,
    title                             VARCHAR(150) NOT NULL,
    lyrics                            TEXT NULL,
    genre                             VARCHAR(100) NOT NULL,
    audio_key                         VARCHAR(255) NOT NULL,        -- V11: audio_url → audio_key
    duration_sec                      BIGINT       NOT NULL,
    featured_artist_text              VARCHAR(255) NULL,
    publish_delay_days                INT NULL,
    status                            VARCHAR(20)  NOT NULL,
    reviewed_by_admin_id              BIGINT NULL,
    reviewed_at                       datetime NULL,
    rejection_reason                  VARCHAR(255) NULL,
    updated_at                        datetime     NOT NULL,
    -- V8 + V11: FREE_CREATION PENDING 중복 방지 (audio_url 참조 제거 후 title 기반으로 재생성됨)
    free_creation_pending_key         CHAR(64)
        GENERATED ALWAYS AS (
            CASE
                WHEN track_type = 'FREE_CREATION'
                    AND status = 'PENDING'
                    THEN SHA2(
                        CONCAT_WS(
                            CHAR(31),
                                requester_user_id,
                                CHAR_LENGTH(COALESCE(title, '')),
                                COALESCE(title, '')
                        ),
                        256
                         )
                ELSE NULL
                END
            ) STORED,
    -- V8: OFFICIAL_RELEASE 정책별 중복 방지 (track_number 기준)
    official_release_track_number_key VARCHAR(500)
        GENERATED ALWAYS AS (
            CASE
                WHEN track_type = 'OFFICIAL_RELEASE'
                    AND status IN ('PENDING', 'APPROVED')
                    THEN CONCAT(
                        requester_user_id,
                        ':',
                        COALESCE(artist_id, 'NULL'),
                        ':',
                        COALESCE(album_id, 'NULL'),
                        ':',
                        COALESCE(track_number, 'NULL')
                         )
                ELSE NULL
                END
            ) STORED,
    -- V8: OFFICIAL_RELEASE 정책별 중복 방지 (title 기준)
    official_release_title_key        VARCHAR(500)
        GENERATED ALWAYS AS (
            CASE
                WHEN track_type = 'OFFICIAL_RELEASE'
                    AND status IN ('PENDING', 'APPROVED')
                    THEN CONCAT(
                        requester_user_id,
                        ':',
                        COALESCE(artist_id, 'NULL'),
                        ':',
                        COALESCE(album_id, 'NULL'),
                        ':',
                        title
                         )
                ELSE NULL
                END
            ) STORED,
    CONSTRAINT pk_track_applications PRIMARY KEY (id)
);

CREATE TABLE track_comments
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    created_at datetime NULL,
    user_id    BIGINT        NOT NULL,
    track_id   BIGINT        NOT NULL,
    content    VARCHAR(1000) NOT NULL,                              -- V5: TEXT → VARCHAR(1000), status 컬럼 제거
    updated_at datetime      NOT NULL,
    deleted_at datetime NULL,
    CONSTRAINT pk_track_comments PRIMARY KEY (id)
);


-- =============================================================================
-- 플레이리스트 도메인
-- =============================================================================

CREATE TABLE playlists
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    created_at    datetime NULL,
    user_id       BIGINT       NOT NULL,
    title         VARCHAR(100) NOT NULL,
    `description` VARCHAR(255) NULL,
    deleted       BIT(1)       NOT NULL,                            -- V5: 활성 데이터 기준 unique 위한 플래그
    updated_at    datetime NULL,
    deleted_at    datetime NULL,
    CONSTRAINT pk_playlists PRIMARY KEY (id)
);

CREATE TABLE playlist_tracks
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    created_at  datetime NULL,
    playlist_id BIGINT NOT NULL,
    track_id    BIGINT NOT NULL,
    position    INT    NOT NULL,
    CONSTRAINT pk_playlist_tracks PRIMARY KEY (id)
);


-- =============================================================================
-- Playback / 인기 차트 / 추천 / 검색
-- =============================================================================

CREATE TABLE playbacks
(
    id              BIGINT AUTO_INCREMENT NOT NULL,
    playlist_id     BIGINT       NOT NULL,
    track_id        BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    session_id      VARCHAR(255) NOT NULL,
    device_id       VARCHAR(100) NULL,
    status          VARCHAR(255) NOT NULL,
    played_duration INT          NOT NULL,
    started_at      datetime     NOT NULL,
    last_played_at  datetime     NOT NULL,
    ended_at        datetime NULL,
    CONSTRAINT pk_playbacks PRIMARY KEY (id)
);

CREATE TABLE popular_charts
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    track_id      BIGINT   NOT NULL,
    chart_rank    INT      NOT NULL,
    play_count    BIGINT   NOT NULL,
    snapshot_date datetime NOT NULL,
    CONSTRAINT pk_popular_charts PRIMARY KEY (id)
);

CREATE TABLE recommendations
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    created_at datetime NULL,
    user_id    BIGINT NOT NULL,
    track_id   BIGINT NOT NULL,
    score      BIGINT NULL,
    reason     VARCHAR(255) NULL,
    CONSTRAINT pk_recommendations PRIMARY KEY (id)
);

CREATE TABLE search_histories
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    created_at   datetime NULL,
    user_id      BIGINT NULL,
    keyword      VARCHAR(255) NOT NULL,
    result_count INT NULL,
    CONSTRAINT pk_search_histories PRIMARY KEY (id)
);


-- =============================================================================
-- 좋아요 / 팔로우 / 알림
-- =============================================================================

CREATE TABLE likes
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    created_at  datetime NULL,
    user_id     BIGINT       NOT NULL,
    target_id   BIGINT       NOT NULL,
    target_type VARCHAR(255) NOT NULL,
    CONSTRAINT pk_likes PRIMARY KEY (id)
);

CREATE TABLE follows
(
    id                   BIGINT AUTO_INCREMENT NOT NULL,
    created_at           datetime NULL,
    artist_id            BIGINT NOT NULL,
    user_id              BIGINT NOT NULL,
    notification_enabled BIT(1) NOT NULL,
    CONSTRAINT pk_follows PRIMARY KEY (id)
);

CREATE TABLE notifications
(
    id              BIGINT AUTO_INCREMENT NOT NULL,
    created_at      datetime NULL,
    user_id         BIGINT       NOT NULL,
    type            VARCHAR(255) NOT NULL,
    content         VARCHAR(255) NOT NULL,
    status          VARCHAR(255) NOT NULL,
    channel         VARCHAR(255) NOT NULL,
    actor_id        BIGINT NULL,                                    -- V6
    resource_id     BIGINT NULL,                                    -- V6
    idempotency_key VARCHAR(255) NULL,                              -- V6: 멱등성 키
    read_at         datetime NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id)
);

-- V6: 메모리 큐 유실 방지를 위한 Outbox 패턴
CREATE TABLE notification_outbox
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    event_type     VARCHAR(50)  NOT NULL,
    target_user_id BIGINT       NOT NULL,
    actor_id       BIGINT       NULL,
    resource_id    BIGINT       NULL,
    content        VARCHAR(500) NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count    INT          NOT NULL DEFAULT 0,
    created_at     DATETIME     NOT NULL,
    processed_at   DATETIME     NULL,
    CONSTRAINT pk_notification_outbox PRIMARY KEY (id)
);


-- =============================================================================
-- 지갑 / 포인트
-- =============================================================================

CREATE TABLE wallets
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    created_at    datetime NULL,
    user_id       BIGINT   NOT NULL,
    balance       BIGINT   NOT NULL,
    event_balance BIGINT   NOT NULL,
    total_balance BIGINT   NOT NULL,
    updated_at    datetime NOT NULL,
    CONSTRAINT pk_wallets PRIMARY KEY (id)
);

CREATE TABLE point_histories
(
    id                 BIGINT AUTO_INCREMENT NOT NULL,
    created_at         datetime NULL,
    wallet_id          BIGINT       NOT NULL,
    point_type         VARCHAR(255) NOT NULL,
    point_history_type VARCHAR(255) NOT NULL,
    amount             BIGINT       NOT NULL,
    balance_after      BIGINT       NOT NULL,
    log_description    VARCHAR(255) NOT NULL,
    cash_order_id      BIGINT NULL,                                 -- V2
    point_order_id     BIGINT NULL,                                 -- V2
    CONSTRAINT pk_point_histories PRIMARY KEY (id)
);

CREATE TABLE point_orders
(
    id                  BIGINT AUTO_INCREMENT NOT NULL,
    created_at          datetime NULL,
    user_id             BIGINT       NOT NULL,
    plan_type           VARCHAR(255) NOT NULL,
    subscription_amount BIGINT       NOT NULL,
    order_number        VARCHAR(255) NOT NULL,
    status              VARCHAR(255) NOT NULL,
    CONSTRAINT pk_point_orders PRIMARY KEY (id)
);

CREATE TABLE cash_orders
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    created_at   datetime NULL,
    user_id      BIGINT       NOT NULL,
    product_type VARCHAR(255) NOT NULL,
    cash_amount  BIGINT       NOT NULL,
    point_amount BIGINT       NOT NULL,
    order_number VARCHAR(50)  NOT NULL,
    status       VARCHAR(255) NOT NULL,
    webhook_id   VARCHAR(255) NULL,
    CONSTRAINT pk_cash_orders PRIMARY KEY (id)
);


-- =============================================================================
-- 결제 / 구독 / 빌링키 / 웹훅
-- =============================================================================

CREATE TABLE payments
(
    id                BIGINT AUTO_INCREMENT NOT NULL,
    created_at        datetime NULL,
    user_id           BIGINT       NOT NULL,
    amount            BIGINT       NOT NULL,
    status            VARCHAR(255) NOT NULL,
    order_number      VARCHAR(50)  NOT NULL,
    pg_transaction_id VARCHAR(100) NOT NULL,
    webhook_id        VARCHAR(100) NOT NULL,
    refund_reason     VARCHAR(500) NULL,
    paid_at           datetime NULL,
    refunded_at       datetime NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id)
);

-- V1 + V10: user_id 컬럼 제거됨 / V5: expiry_date NULL 허용
CREATE TABLE subscriptions
(
    id                BIGINT AUTO_INCREMENT NOT NULL,
    created_at        datetime NULL,
    point_order_id    BIGINT       NOT NULL,
    plan_type         VARCHAR(255) NOT NULL,
    status            VARCHAR(255) NOT NULL,
    start_date        datetime     NOT NULL,
    expiry_date       datetime NULL,
    next_billing_date datetime NULL,
    CONSTRAINT pk_subscriptions PRIMARY KEY (id)
);

-- V2
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

-- V4
CREATE TABLE billing_attempts
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    created_at     datetime    NULL,
    user_id        BIGINT      NOT NULL,
    billing_key_id BIGINT      NOT NULL,
    billing_cycle  VARCHAR(7)  NOT NULL,
    status         VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(50) NULL,
    attempted_at   datetime    NOT NULL,
    CONSTRAINT pk_billing_attempts PRIMARY KEY (id)
);

-- V9: PortOne 웹훅 중복 수신 방지
CREATE TABLE webhook_events
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    created_at       datetime     NULL,
    webhook_event_id VARCHAR(255) NOT NULL,
    payment_id       VARCHAR(255) NOT NULL,
    received_at      datetime     NOT NULL,
    CONSTRAINT pk_webhook_events PRIMARY KEY (id)
);


-- =============================================================================
-- 채팅 도메인 (V5)
-- =============================================================================

CREATE TABLE chat_sessions
(
    id                       BIGINT AUTO_INCREMENT NOT NULL,
    created_at               datetime NULL,
    user_id                  BIGINT NOT NULL,
    title                    VARCHAR(255) NULL,
    summary                  TEXT NULL,
    summary_until_message_id BIGINT NULL,
    updated_at               datetime NULL,
    CONSTRAINT pk_chat_sessions PRIMARY KEY (id)
);

CREATE TABLE chat_messages
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    created_at datetime NULL,
    session_id BIGINT       NOT NULL,
    `role`     VARCHAR(255) NOT NULL,
    content    TEXT         NOT NULL,
    CONSTRAINT pk_chat_messages PRIMARY KEY (id)
);

CREATE TABLE chat_message_tracks
(
    message_id    BIGINT NOT NULL,
    track_id      BIGINT NOT NULL,
    display_order INT    NOT NULL,
    CONSTRAINT pk_chat_message_tracks PRIMARY KEY (message_id, track_id)
);


-- =============================================================================
-- UNIQUE 제약 (V1 기본 + 후속 추가)
-- =============================================================================

-- V1
ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

-- V1
ALTER TABLE likes
    ADD CONSTRAINT uc_4aa1641303ec82c61244461af UNIQUE (user_id, target_id, target_type);

-- V1
ALTER TABLE follows
    ADD CONSTRAINT uc_85b12db86c1c2d5b314903941 UNIQUE (user_id, artist_id);

-- V1
ALTER TABLE cash_orders
    ADD CONSTRAINT uc_cash_orders_ordernumber UNIQUE (order_number);

ALTER TABLE cash_orders
    ADD CONSTRAINT uc_cash_orders_webhookid UNIQUE (webhook_id);

-- V1
ALTER TABLE payments
    ADD CONSTRAINT uc_payments_pgtransactionid UNIQUE (pg_transaction_id);

ALTER TABLE payments
    ADD CONSTRAINT uc_payments_webhookid UNIQUE (webhook_id);

-- V1
ALTER TABLE playlist_tracks
    ADD CONSTRAINT uk_playlist_track_playlist_position UNIQUE (playlist_id, position);

ALTER TABLE playlist_tracks
    ADD CONSTRAINT uk_playlist_track_playlist_track UNIQUE (playlist_id, track_id);

-- V1
ALTER TABLE popular_charts
    ADD CONSTRAINT uk_snapshot_track UNIQUE (snapshot_date, track_id);

-- V5: chart_rank 기준 unique (snapshot 무결성)
ALTER TABLE popular_charts
    ADD CONSTRAINT uk_popular_chart_snapshot_rank UNIQUE (snapshot_date, chart_rank);

-- V2: point_histories 복합 unique
ALTER TABLE point_histories
    ADD CONSTRAINT uc_point_histories_cash_order_history
        UNIQUE (cash_order_id, point_history_type);

ALTER TABLE point_histories
    ADD CONSTRAINT uc_point_histories_point_order_history
        UNIQUE (point_order_id, point_history_type);

-- V5: 활성 데이터 기준 playlist 제목 중복 금지
ALTER TABLE playlists
    ADD CONSTRAINT uk_playlist_user_title_deleted UNIQUE (user_id, title, deleted);

-- V6: 알림 멱등성 키
ALTER TABLE notifications
    ADD CONSTRAINT uc_notifications_idempotency_key UNIQUE (idempotency_key);

-- V7: Application 다층 방어선 — 2차 (신청 row 중복 TOCTOU)
ALTER TABLE album_applications
    ADD UNIQUE INDEX uq_album_application_active_duplicate_key (active_duplicate_key);

ALTER TABLE artist_applications
    ADD UNIQUE INDEX uq_artist_application_active_duplicate_key (active_duplicate_key);

-- V8: Application 다층 방어선 — 최종 (승인 결과 엔티티 중복 방지)
ALTER TABLE tracks
    ADD UNIQUE INDEX uq_tracks_track_application_id (track_application_id);

ALTER TABLE albums
    ADD UNIQUE INDEX uq_albums_album_application_id (album_application_id);

ALTER TABLE artists
    ADD UNIQUE INDEX uq_artists_artist_application_id (artist_application_id);

-- V8 + V11: TrackApplication 정책별 중복 방지
ALTER TABLE track_applications
    ADD UNIQUE INDEX uq_track_applications_free_creation_pending (free_creation_pending_key);

ALTER TABLE track_applications
    ADD UNIQUE INDEX uq_track_applications_official_release_track_number (official_release_track_number_key);

ALTER TABLE track_applications
    ADD UNIQUE INDEX uq_track_applications_official_release_title (official_release_title_key);

-- V9: PortOne 웹훅 중복 수신 방지
ALTER TABLE webhook_events
    ADD CONSTRAINT uc_webhook_events_webhook_event_id
        UNIQUE (webhook_event_id);

ALTER TABLE webhook_events
    ADD CONSTRAINT uc_webhook_events_payment_id
        UNIQUE (payment_id);


-- =============================================================================
-- 인덱스 (V1 기본 + 후속 추가)
-- =============================================================================

-- V1: album / album_applications
CREATE INDEX idx_album_application_artist_id           ON album_applications (artist_id);
CREATE INDEX idx_album_application_requester_user_id   ON album_applications (requester_user_id);
CREATE INDEX idx_album_application_status              ON album_applications (status);
CREATE INDEX idx_album_artist_id                       ON albums (artist_id);
CREATE INDEX idx_album_status                          ON albums (status);

-- V1: artist / artist_applications
CREATE INDEX idx_artist_application_requester_user_id  ON artist_applications (requester_user_id);
CREATE INDEX idx_artist_application_status             ON artist_applications (status);
CREATE INDEX idx_artist_owner_deleted_created          ON artists (owner_user_id, deleted_at, created_at);

-- V1: likes
CREATE INDEX idx_likes_user_id                         ON likes (user_id);

-- V1: playback
CREATE INDEX idx_playback_ended_at                     ON playbacks (ended_at);

-- V1: search_histories
CREATE INDEX idx_search_histories_created_at           ON search_histories (created_at);
CREATE INDEX idx_search_histories_user_id              ON search_histories (user_id);

-- V1: tracks / track_applications / track_comments
CREATE INDEX idx_track_album_id                        ON tracks (album_id);
CREATE INDEX idx_track_application_album_id            ON track_applications (album_id);
CREATE INDEX idx_track_application_artist_id           ON track_applications (artist_id);
CREATE INDEX idx_track_application_requester_user_id   ON track_applications (requester_user_id);
CREATE INDEX idx_track_application_status              ON track_applications (status);
CREATE INDEX idx_track_artist_id                       ON tracks (artist_id);
CREATE INDEX idx_track_comment_track_id                ON track_comments (track_id);
CREATE INDEX idx_track_comment_user_id                 ON track_comments (user_id);
CREATE INDEX idx_track_owner_user_id                   ON tracks (owner_user_id);
CREATE INDEX idx_track_status                          ON tracks (status);

-- V3: 성능 인덱스 (filesort 제거 / 응답속도 약 25배 단축)
CREATE INDEX idx_track_status_deleted_published        ON tracks (status, deleted_at, published_at DESC);
CREATE INDEX idx_track_comment_track_deleted_created   ON track_comments (track_id, deleted_at, created_at DESC);

-- V4: billing_attempts
CREATE INDEX idx_billing_attempts_user_id              ON billing_attempts (user_id);
CREATE INDEX idx_billing_attempts_billing_key_id       ON billing_attempts (billing_key_id);

-- V5: playlists
CREATE INDEX idx_playlist_deleted_at                   ON playlists (deleted_at);
CREATE INDEX idx_playlist_user_deleted_at              ON playlists (user_id, deleted_at);
CREATE INDEX idx_playlist_user_title_deleted_at        ON playlists (user_id, title, deleted_at);

-- V6: notification_outbox (Worker polling)
CREATE INDEX idx_outbox_status_created                 ON notification_outbox (status, created_at);

-- V9: webhook_events
CREATE INDEX idx_webhook_events_payment_id             ON webhook_events (payment_id);