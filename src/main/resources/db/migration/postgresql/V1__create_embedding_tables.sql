CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE track_embedding (
    track_id        BIGINT       PRIMARY KEY,
    embedding       VECTOR(1024) NOT NULL,
    source_text     TEXT         NOT NULL,
    source_hash     VARCHAR(64)  NOT NULL,
    embedded_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    model_version   VARCHAR(50)  NOT NULL
);

CREATE INDEX idx_track_embedding_cosine
    ON track_embedding
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

CREATE INDEX idx_track_embedding_hash ON track_embedding(source_hash);

CREATE TABLE user_embedding (
    user_id         BIGINT       PRIMARY KEY,
    embedding       VECTOR(1024) NOT NULL,
    based_on_count  INT          NOT NULL,
    computed_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_embedding_cosine
    ON user_embedding
    USING hnsw (embedding vector_cosine_ops);

CREATE TABLE embedding_job_log (
    id              BIGSERIAL    PRIMARY KEY,
    job_type        VARCHAR(30)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    processed_count INT          DEFAULT 0,
    failed_count    INT          DEFAULT 0,
    started_at      TIMESTAMP    NOT NULL,
    finished_at     TIMESTAMP,
    error_message   TEXT
);

CREATE TABLE track_lyrics_embedding (
    track_id        BIGINT       PRIMARY KEY,
    embedding       VECTOR(1024) NOT NULL,
    snippet         VARCHAR(50)  NOT NULL,
    chunk_count     INT          NOT NULL,
    source_hash     VARCHAR(64)  NOT NULL,
    embedded_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    model_version   VARCHAR(50)  NOT NULL
);

CREATE INDEX idx_lyrics_embedding_cosine
    ON track_lyrics_embedding
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

CREATE INDEX idx_lyrics_embedding_hash ON track_lyrics_embedding(source_hash);
