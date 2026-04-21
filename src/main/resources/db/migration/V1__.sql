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
    CONSTRAINT pk_album_applications PRIMARY KEY (id)
);

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
    updated_at           datetime     NOT NULL,
    deleted_at           datetime NULL,
    CONSTRAINT pk_albums PRIMARY KEY (id)
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
    CONSTRAINT pk_artist_applications PRIMARY KEY (id)
);

CREATE TABLE artists
(
    id                BIGINT AUTO_INCREMENT NOT NULL,
    created_at        datetime NULL,
    owner_user_id     BIGINT       NOT NULL,
    name              VARCHAR(100) NOT NULL,
    artist_type       VARCHAR(20)  NOT NULL,
    bio               TEXT NULL,
    profile_image_url VARCHAR(255) NULL,
    status            VARCHAR(20)  NOT NULL,
    updated_at        datetime     NOT NULL,
    deleted_at        datetime NULL,
    CONSTRAINT pk_artists PRIMARY KEY (id)
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

CREATE TABLE follows
(
    id                   BIGINT AUTO_INCREMENT NOT NULL,
    created_at           datetime NULL,
    artist_id            BIGINT NOT NULL,
    user_id              BIGINT NOT NULL,
    notification_enabled BIT(1) NOT NULL,
    CONSTRAINT pk_follows PRIMARY KEY (id)
);

CREATE TABLE likes
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    created_at  datetime NULL,
    user_id     BIGINT       NOT NULL,
    target_id   BIGINT       NOT NULL,
    target_type VARCHAR(255) NOT NULL,
    CONSTRAINT pk_likes PRIMARY KEY (id)
);

CREATE TABLE notifications
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    created_at datetime NULL,
    user_id    BIGINT       NOT NULL,
    type       VARCHAR(255) NOT NULL,
    content    VARCHAR(255) NOT NULL,
    status     VARCHAR(255) NOT NULL,
    channel    VARCHAR(255) NOT NULL,
    read_at    datetime NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id)
);

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

CREATE TABLE playlist_tracks
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    created_at  datetime NULL,
    playlist_id BIGINT NOT NULL,
    track_id    BIGINT NOT NULL,
    position    INT    NOT NULL,
    CONSTRAINT pk_playlist_tracks PRIMARY KEY (id)
);

CREATE TABLE playlists
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    created_at    datetime NULL,
    user_id       BIGINT       NOT NULL,
    title         VARCHAR(100) NOT NULL,
    `description` VARCHAR(255) NULL,
    updated_at    datetime NULL,
    deleted_at    datetime NULL,
    CONSTRAINT pk_playlists PRIMARY KEY (id)
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

CREATE TABLE subscriptions
(
    id                BIGINT AUTO_INCREMENT NOT NULL,
    created_at        datetime NULL,
    user_id           BIGINT       NOT NULL,
    point_order_id    BIGINT       NOT NULL,
    plan_type         VARCHAR(255) NOT NULL,
    status            VARCHAR(255) NOT NULL,
    start_date        datetime     NOT NULL,
    expiry_date       datetime     NOT NULL,
    next_billing_date datetime NULL,
    CONSTRAINT pk_subscriptions PRIMARY KEY (id)
);

CREATE TABLE track_applications
(
    id                   BIGINT AUTO_INCREMENT NOT NULL,
    created_at           datetime NULL,
    requester_user_id    BIGINT       NOT NULL,
    track_type           VARCHAR(30)  NOT NULL,
    artist_id            BIGINT NULL,
    album_id             BIGINT NULL,
    track_number         BIGINT NULL,
    title                VARCHAR(150) NOT NULL,
    lyrics               TEXT NULL,
    genre                VARCHAR(100) NOT NULL,
    audio_url            VARCHAR(255) NOT NULL,
    duration_sec         BIGINT       NOT NULL,
    featured_artist_text VARCHAR(255) NULL,
    publish_delay_days   INT NULL,
    status               VARCHAR(20)  NOT NULL,
    reviewed_by_admin_id BIGINT NULL,
    reviewed_at          datetime NULL,
    rejection_reason     VARCHAR(255) NULL,
    updated_at           datetime     NOT NULL,
    CONSTRAINT pk_track_applications PRIMARY KEY (id)
);

CREATE TABLE track_comments
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    created_at datetime NULL,
    user_id    BIGINT       NOT NULL,
    track_id   BIGINT       NOT NULL,
    content    TEXT         NOT NULL,
    status     VARCHAR(255) NOT NULL,
    updated_at datetime     NOT NULL,
    deleted_at datetime NULL,
    CONSTRAINT pk_track_comments PRIMARY KEY (id)
);

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
    audio_url            VARCHAR(255) NOT NULL,
    duration_sec         BIGINT       NOT NULL,
    featured_artist_text VARCHAR(255) NULL,
    status               VARCHAR(20)  NOT NULL,
    scheduled_publish_at datetime NULL,
    published_at         datetime NULL,
    play_count           BIGINT       NOT NULL,
    updated_at           datetime     NOT NULL,
    deleted_at           datetime NULL,
    CONSTRAINT pk_tracks PRIMARY KEY (id)
);

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

ALTER TABLE likes
    ADD CONSTRAINT uc_4aa1641303ec82c61244461af UNIQUE (user_id, target_id, target_type);

ALTER TABLE follows
    ADD CONSTRAINT uc_85b12db86c1c2d5b314903941 UNIQUE (user_id, artist_id);

ALTER TABLE cash_orders
    ADD CONSTRAINT uc_cash_orders_ordernumber UNIQUE (order_number);

ALTER TABLE cash_orders
    ADD CONSTRAINT uc_cash_orders_webhookid UNIQUE (webhook_id);

ALTER TABLE payments
    ADD CONSTRAINT uc_payments_pgtransactionid UNIQUE (pg_transaction_id);

ALTER TABLE payments
    ADD CONSTRAINT uc_payments_webhookid UNIQUE (webhook_id);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE playlist_tracks
    ADD CONSTRAINT uk_playlist_track_playlist_position UNIQUE (playlist_id, position);

ALTER TABLE playlist_tracks
    ADD CONSTRAINT uk_playlist_track_playlist_track UNIQUE (playlist_id, track_id);

ALTER TABLE popular_charts
    ADD CONSTRAINT uk_snapshot_track UNIQUE (snapshot_date, track_id);

CREATE INDEX idx_album_application_artist_id ON album_applications (artist_id);

CREATE INDEX idx_album_application_requester_user_id ON album_applications (requester_user_id);

CREATE INDEX idx_album_application_status ON album_applications (status);

CREATE INDEX idx_album_artist_id ON albums (artist_id);

CREATE INDEX idx_album_status ON albums (status);

CREATE INDEX idx_artist_application_requester_user_id ON artist_applications (requester_user_id);

CREATE INDEX idx_artist_application_status ON artist_applications (status);

CREATE INDEX idx_artist_owner_deleted_created ON artists (owner_user_id, deleted_at, created_at);

CREATE INDEX idx_likes_user_id ON likes (user_id);

CREATE INDEX idx_playback_ended_at ON playbacks (ended_at);

CREATE INDEX idx_search_histories_created_at ON search_histories (created_at);

CREATE INDEX idx_search_histories_user_id ON search_histories (user_id);

CREATE INDEX idx_track_album_id ON tracks (album_id);

CREATE INDEX idx_track_application_album_id ON track_applications (album_id);

CREATE INDEX idx_track_application_artist_id ON track_applications (artist_id);

CREATE INDEX idx_track_application_requester_user_id ON track_applications (requester_user_id);

CREATE INDEX idx_track_application_status ON track_applications (status);

CREATE INDEX idx_track_artist_id ON tracks (artist_id);

CREATE INDEX idx_track_comment_track_id ON track_comments (track_id);

CREATE INDEX idx_track_comment_user_id ON track_comments (user_id);

CREATE INDEX idx_track_owner_user_id ON tracks (owner_user_id);

CREATE INDEX idx_track_status ON tracks (status);