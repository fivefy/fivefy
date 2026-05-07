ALTER TABLE album_applications
    ADD COLUMN active_duplicate_key VARCHAR(500)
        GENERATED ALWAYS AS (
            CASE
                WHEN status IN ('PENDING', 'APPROVED')
                    THEN CONCAT(requester_user_id, ':', artist_id, ':', title)
                ELSE NULL
                END
            ) STORED;

ALTER TABLE album_applications
    ADD UNIQUE INDEX uq_album_application_active_duplicate_key (active_duplicate_key),
    ALGORITHM=INPLACE,
    LOCK=NONE;

ALTER TABLE artist_applications
    ADD COLUMN active_duplicate_key VARCHAR(500)
        GENERATED ALWAYS AS (
            CASE
                WHEN status IN ('PENDING', 'APPROVED')
                    THEN CONCAT(requester_user_id, ':', requested_name, ':', artist_type)
                ELSE NULL
                END
            ) STORED;

ALTER TABLE artist_applications
    ADD UNIQUE INDEX uq_artist_application_active_duplicate_key (active_duplicate_key),
    ALGORITHM=INPLACE,
    LOCK=NONE;
