ALTER TABLE tracks
    ADD INDEX idx_track_status_deleted_published (status, deleted_at, published_at DESC),
    ALGORITHM=INPLACE,
    LOCK=NONE;

ALTER TABLE track_comments
    ADD INDEX idx_track_comment_track_deleted_created (track_id, deleted_at, created_at DESC),
    ALGORITHM=INPLACE,
    LOCK=NONE;
