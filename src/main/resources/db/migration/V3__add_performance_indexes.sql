CREATE INDEX idx_track_status_deleted_published
    ON tracks (status, deleted_at, published_at DESC);

CREATE INDEX idx_track_comment_track_deleted_created
    ON track_comments (track_id, deleted_at, created_at DESC);