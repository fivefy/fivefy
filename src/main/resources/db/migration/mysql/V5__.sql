CREATE TABLE chat_message_tracks
(
    message_id    BIGINT NOT NULL,
    track_id      BIGINT NOT NULL,
    display_order INT    NOT NULL,
    CONSTRAINT pk_chat_message_tracks PRIMARY KEY (message_id, track_id)
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

ALTER TABLE playlists
    ADD deleted BIT(1) NULL;

ALTER TABLE playlists
    MODIFY deleted BIT (1) NOT NULL;

ALTER TABLE playlists
    ADD CONSTRAINT uk_playlist_user_title_deleted UNIQUE (user_id, title, deleted);

ALTER TABLE popular_charts
    ADD CONSTRAINT uk_popular_chart_snapshot_rank UNIQUE (snapshot_date, chart_rank);

CREATE INDEX idx_playlist_deleted_at ON playlists (deleted_at);

CREATE INDEX idx_playlist_user_deleted_at ON playlists (user_id, deleted_at);

CREATE INDEX idx_playlist_user_title_deleted_at ON playlists (user_id, title, deleted_at);

ALTER TABLE track_comments
DROP
COLUMN status;

ALTER TABLE artists
    MODIFY bio VARCHAR (1000);

ALTER TABLE track_comments
    MODIFY content VARCHAR (1000);

ALTER TABLE subscriptions
    MODIFY expiry_date datetime NULL;

ALTER TABLE artists
    MODIFY profile_image_url VARCHAR (256);